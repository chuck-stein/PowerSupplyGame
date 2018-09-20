import java.util.*;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

/*-
 * EXTRA FEATURES:
 * the color of powered pipes fades the further away they are from power station
 * timer for scorekeeping
 */

// represents a game of LightEmAll
class PowerSupply extends World {
  Random rand;
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of all edges
  ArrayList<Edge> edges;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  int tickNum;

  PowerSupply(int width, int height, Random rand) {
    this.rand = rand;
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Width and height must be positive values.");
    }
    this.width = width;
    this.height = height;
    this.powerRow = 0;
    this.powerCol = 0;
    this.tickNum = 0;
    this.board = this.dummyBoard();
    this.nodes = this.setNodes();
    this.edges = this.setEdges();
    this.mst = this.setMST();
    this.setConnections();
    this.updatePieceEdges();
    this.radius = this.findRadius();
    this.scrambleBoard();
    this.sendPower(this.powerCol, this.powerRow, this.radius);
  }

  PowerSupply(int width, int height) {
    this(width, height, new Random());
  }

  // constructor for testing purposes
  PowerSupply(int width, int height, ArrayList<ArrayList<GamePiece>> board,
      ArrayList<GamePiece> nodes, ArrayList<Edge> edges, Random rand) {
    this.rand = rand;
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Width and height must be positive values.");
    }
    this.width = width;
    this.height = height;
    this.tickNum = 0;
    this.board = board;
    this.nodes = nodes;
    this.edges = edges;
    this.mst = new ArrayList<Edge>();
    this.powerRow = 0;
    this.powerCol = 0;
    this.radius = this.findRadius();
  }

  // on every tick, increases the counter for ticks so far in this game
  public void onTick() {
    this.tickNum += 1;
  }

  // returns a dummy board of gamepieces with no connections
  ArrayList<ArrayList<GamePiece>> dummyBoard() {
    ArrayList<ArrayList<GamePiece>> dummy = new ArrayList<ArrayList<GamePiece>>();
    for (int c = 0; c < this.width; c += 1) {
      dummy.add(new ArrayList<GamePiece>());
      for (int r = 0; r < this.height; r += 1) {
        dummy.get(c).add(new GamePiece(r, c, false, false, false, false, false, this.rand));
      }
    }
    dummy.get(0).get(0).powerStation = true;
    return dummy;
  }

  // returns a list of all gamepieces on the board
  ArrayList<GamePiece> setNodes() {
    ArrayList<GamePiece> nodeList = new ArrayList<GamePiece>();
    for (int c = 0; c < this.width; c += 1) {
      for (int r = 0; r < this.height; r += 1) {
        nodeList.add(this.board.get(c).get(r));
      }
    }

    return nodeList;
  }

  // returns all possible edges between gamepieces, assuming they're all connected
  ArrayList<Edge> setEdges() {
    ArrayList<Edge> edgeList = new ArrayList<Edge>();
    GamePiece curr;
    for (int c = 0; c < this.width; c += 1) {
      for (int r = 0; r < this.height; r += 1) {
        curr = this.board.get(c).get(r);
        if (c > 0) {
          edgeList.add(new Edge(curr, this.board.get(c - 1).get(r), this.rand));
        }
        if (r > 0) {
          edgeList.add(new Edge(curr, this.board.get(c).get(r - 1), this.rand));
        }
        if (c < this.width - 1) {
          edgeList.add(new Edge(curr, this.board.get(c + 1).get(r), this.rand));
        }
        if (r < this.height - 1) {
          edgeList.add(new Edge(curr, this.board.get(c).get(r + 1), this.rand));
        }
      }

    }
    return edgeList;
  }

  // uses Kruskal's Algorithm to find the minimum spanning tree of the board
  ArrayList<Edge> setMST() {
    ArrayList<Edge> tree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = this.heapsortEdges();
    HashMap<GamePiece, GamePiece> map = new HashMap<GamePiece, GamePiece>();
    Edge next;
    for (GamePiece piece : this.nodes) {
      map.put(piece, piece);
    }
    while (this.multipleSelfReps(map)) {
      next = worklist.remove(0);
      // NOTE: using intensional equality on gamepieces because they are mutable
      // and because no two are the same (all have different col/row position)
      if (this.findRep(map, next.fromNode) != this.findRep(map, next.toNode)) {
        tree.add(next);
        map.replace(this.findRep(map, next.toNode), next.fromNode);
      }
    }
    return tree;
  }

  // is there more than one key in the given hashmap whose value is itself?
  boolean multipleSelfReps(HashMap<GamePiece, GamePiece> map) {
    boolean foundSelfRep = false;
    for (GamePiece piece : this.nodes) {
      // NOTE: using intensional equality on gamepieces because they are mutable
      // and because no two are the same (all have different col/row position)
      if (map.get(piece) == piece) {
        if (foundSelfRep) {
          return true;
        }
        else {
          foundSelfRep = true;
        }
      }
    }
    return false;
  }

  // finds the representative of the given piece in the given hashmap
  // (meaning trace back the values to keys until a key points to itself)
  GamePiece findRep(HashMap<GamePiece, GamePiece> map, GamePiece piece) {
    if (map.get(piece) == piece) {
      return piece;
    }
    else {
      return findRep(map, map.get(piece));
    }
  }

  // sorts all possible edges between gamepieces using heapsort
  ArrayList<Edge> heapsortEdges() {
    formHeap(this.edges, this.edges.size() - 1);
    for (int i = this.edges.size() - 1; i > 0; i -= 1) {
      Edge temp = this.edges.get(0);
      this.edges.set(0, this.edges.get(i));
      this.edges.set(i, temp);
      formHeap(this.edges, i - 1);
    }
    return this.edges;
  }

  // makes the given list into a valid heap
  void formHeap(ArrayList<Edge> edgeList, int startIndex) {
    for (int i = startIndex; i > 0; i -= 1) {
      int curr = i;
      while (curr > 0) {
        Edge child = edgeList.get(curr);
        Edge parent = edgeList.get((curr - 1) / 2);
        if (child.weight > parent.weight) {
          edgeList.set(curr, parent);
          edgeList.set((curr - 1) / 2, child);
        }
        curr = (curr - 1) / 2;
      }
    }
  }

  // EFFECT: connects the board's gamepieces together according to the MST
  void setConnections() {
    GamePiece from;
    GamePiece to;
    for (Edge e : this.mst) {
      from = e.fromNode;
      to = e.toNode;
      if (from.col > to.col) {
        from.left = true;
        to.right = true;
      }
      else if (from.row > to.row) {
        from.top = true;
        to.bottom = true;
      }
      else if (from.col < to.col) {
        from.right = true;
        to.left = true;
      }
      else if (from.row < to.row) {
        from.bottom = true;
        to.top = true;
      }
    }
  }

  // EFFECT: randomly rotates all tiles on the board
  void scrambleBoard() {
    int rotations;
    for (int c = 0; c < this.width; c += 1) {
      for (int r = 0; r < this.height; r += 1) {
        rotations = this.rand.nextInt(4);
        while (rotations > 0) {
          this.board.get(c).get(r).rotate();
          rotations -= 1;

        }
      }
    }
  }

  // calculates the radius of the board
  // (half of the furthest distance between nodes, plus 1)
  int findRadius() {
    GamePiece furthestFromStation = this
        .furthestFrom(this.board.get(this.powerCol).get(this.powerRow));
    int diameter = this.findDistance(furthestFromStation, this.furthestFrom(furthestFromStation));
    return diameter / 2 + 1;
  }

  // returns the last gamepiece found from performing a breadth-first search
  // starting at the given gamepiece
  // NOTE: uses an ArrayList with same functionality as a queue
  GamePiece furthestFrom(GamePiece start) {
    ArrayList<GamePiece> queue = new ArrayList<GamePiece>();
    ArrayList<GamePiece> alreadySeen = new ArrayList<GamePiece>();
    queue.add(start);
    while (!queue.isEmpty()) {
      GamePiece next = queue.remove(0);
      if (queue.isEmpty() && next.edges.size() == 1
          && alreadySeen.contains(next.edges.get(0).toNode)) {
        return next;
      }
      else if (!alreadySeen.contains(next)) {
        for (Edge e : next.edges) {
          if (!alreadySeen.contains(e.toNode)) {
            queue.add(e.toNode);
          }
        }
        alreadySeen.add(next);
      }
    }
    return start;
  }

  // finds the shortest distance between the two nodes using BFS
  // NOTE: uses an ArrayList with same functionality as a queue
  int findDistance(GamePiece from, GamePiece to) {
    ArrayList<GamePiece> searchQueue = new ArrayList<GamePiece>();
    ArrayList<Integer> depthQueue = new ArrayList<Integer>();
    ArrayList<GamePiece> alreadySeen = new ArrayList<GamePiece>();
    searchQueue.add(from);
    depthQueue.add(0);
    while (!searchQueue.isEmpty()) {
      GamePiece next = searchQueue.remove(0);
      int depth = depthQueue.remove(0);
      if (next.equals(to)) {
        return depth;
      }
      else if (!alreadySeen.contains(next)) {
        for (Edge e : next.edges) {

          if (!alreadySeen.contains(e.toNode)) {
            searchQueue.add(e.toNode);
            depthQueue.add(depth + 1);
          }
        }
        alreadySeen.add(next);
      }
    }
    return -1; // destination node cannot be reached
  }

  // EFFECT: updates which game pieces are lit based on their connectivity to the
  // power station
  void sendPower(int x, int y, int radiusLeft) {
    // refresh the power grid so no disconnected piece is stuck with power:
    if (radiusLeft == this.radius) {
      for (int c = 0; c < this.width; c += 1) {
        for (int r = 0; r < this.height; r += 1) {
          this.board.get(c).get(r).itsLit = false;
        }
      }
    }
    GamePiece piece = this.board.get(x).get(y);
    piece.itsLit = true;
    // NOTE: the following if statements check if radiusLeft >= 0 instead of
    // radiusLeft > 0 because I am not counting the power station's start tile as
    // expending any power
    if (x > 0 && piece.left && this.board.get(x - 1).get(y).right
        && !this.board.get(x - 1).get(y).itsLit && radiusLeft >= 0) {
      this.sendPower(x - 1, y, radiusLeft - 1);
    }
    if (y > 0 && piece.top && this.board.get(x).get(y - 1).bottom
        && !this.board.get(x).get(y - 1).itsLit && radiusLeft >= 0) {
      this.sendPower(x, y - 1, radiusLeft - 1);
    }
    if (x < this.width - 1 && piece.right && this.board.get(x + 1).get(y).left
        && !this.board.get(x + 1).get(y).itsLit && radiusLeft >= 0) {
      this.sendPower(x + 1, y, radiusLeft - 1);
    }
    if (y < this.height - 1 && piece.bottom && this.board.get(x).get(y + 1).top
        && !this.board.get(x).get(y + 1).itsLit && radiusLeft >= 0) {
      this.sendPower(x, y + 1, radiusLeft - 1);
    }

  }

  // EFFECT: updates the list of outgoing edges for every gamepiece on the board,
  // based on their connections
  void updatePieceEdges() {
    for (int c = 0; c < this.width; c += 1) {
      for (int r = 0; r < this.height; r += 1) {
        GamePiece piece = this.board.get(c).get(r);
        piece.edges = new ArrayList<Edge>();
        if (c > 0 && piece.left && this.board.get(c - 1).get(r).right) {
          piece.addEdge(this.board.get(c - 1).get(r));
        }
        if (r > 0 && piece.top && this.board.get(c).get(r - 1).bottom) {
          piece.addEdge(this.board.get(c).get(r - 1));
        }
        if (c < this.width - 1 && piece.right && this.board.get(c + 1).get(r).left) {
          piece.addEdge(this.board.get(c + 1).get(r));
        }
        if (r < this.height - 1 && piece.bottom && this.board.get(c).get(r + 1).top) {
          piece.addEdge(this.board.get(c).get(r + 1));
        }
      }
    }

  }

  // are all gamepieces receiving power from the power station?
  boolean gameWon() {
    for (int c = 0; c < this.width; c += 1) {
      for (int r = 0; r < this.height; r += 1) {
        if (!this.board.get(c).get(r).itsLit) {
          return false;
        }
      }
    }

    return true;
  }

  // displays the grid of gamepieces
  public WorldScene makeScene() {
    WorldScene scene = this.getEmptyScene();
    GamePiece piece;
    for (int c = 0; c < this.width; c += 1) {
      for (int r = 0; r < this.height; r += 1) {
        piece = this.board.get(c).get(r);
        scene.placeImageXY(
            piece.draw(this.findDistance(piece, this.board.get(this.powerCol).get(this.powerRow)),
                this.radius),
            c * 50 + 25, r * 50 + 25);

      }
    }
    scene.placeImageXY(new TextImage("Time: " + this.tickNum, 24, Color.WHITE), scene.width / 2,
        15);
    return scene;
  }

  // displays the grid of gamepieces and a message indicating the game is over
  public WorldScene lastScene(String msg) {
    WorldScene scene = this.makeScene();
    scene.placeImageXY(new TextImage(msg, 60, Color.GREEN), scene.width / 2, scene.height / 2);
    return scene;
  }

  // EFFECT: handles user mouse clicks, specifically by rotating game pieces and
  // updating the resulting graph
  public void onMouseClicked(Posn p) {
    if (p.x > 0 && p.x < this.width * 50 && p.y > 0 && p.y < this.height * 50) {
      this.board.get(p.x / 50).get(p.y / 50).rotate();
      this.updatePieceEdges();
      this.sendPower(this.powerCol, this.powerRow, this.radius);
      if (this.gameWon()) {
        this.endOfWorld("YOU WIN!");
      }
    }
  }

  // EFFECT: handles user key presses, specifically by moving the power station
  public void onKeyEvent(String key) {
    GamePiece stationPiece = this.board.get(this.powerCol).get(this.powerRow);
    if (key.equals("left")) {
      if (this.powerCol > 0 && this.board.get(this.powerCol - 1).get(this.powerRow).right
          && stationPiece.left) {
        stationPiece.powerStation = false;
        this.powerCol -= 1;
      }
    }
    else if (key.equals("up")) {
      if (this.powerRow > 0 && this.board.get(this.powerCol).get(this.powerRow - 1).bottom
          && stationPiece.top) {
        stationPiece.powerStation = false;
        this.powerRow -= 1;
      }
    }
    else if (key.equals("right")) {
      if (this.powerCol < this.width - 1
          && this.board.get(this.powerCol + 1).get(this.powerRow).left && stationPiece.right) {
        stationPiece.powerStation = false;
        this.powerCol += 1;
      }
    }
    else if (key.equals("down")) {
      if (this.powerRow < this.height - 1
          && this.board.get(this.powerCol).get(this.powerRow + 1).top && stationPiece.bottom) {
        stationPiece.powerStation = false;
        this.powerRow += 1;
      }
    }
    this.board.get(powerCol).get(powerRow).powerStation = true;
    this.sendPower(this.powerCol, this.powerRow, this.radius);
    if (this.gameWon()) {
      this.endOfWorld("YOU WIN!");
    }
  }

}

// represents a tile in the game, which can connect to any of its surrounding
// tiles and be powered or unpowered
class GamePiece {
  Random rand;
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean top;
  boolean right;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  // is this piece receiving power from the power station?
  boolean itsLit;
  ArrayList<Edge> edges;

  GamePiece(int row, int col, boolean left, boolean top, boolean right, boolean bottom,
      boolean powerStation, Random rand) {
    this.rand = rand;
    this.row = row;
    this.col = col;
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.itsLit = false;
    this.edges = new ArrayList<Edge>();
  }

  // EFFECT: rotates this game piece 90 degrees clockwise, updating all of its
  // connections accordingly
  void rotate() {
    boolean prevLeft = this.left;
    boolean prevTop = this.top;
    boolean prevRight = this.right;
    this.left = this.bottom;
    this.top = prevLeft;
    this.right = prevTop;
    this.bottom = prevRight;
  }

  // EFFECT: adds an edge (from this gamepiece to the given one) to this
  // gamepiece's list of edges
  void addEdge(GamePiece piece) {
    this.edges.add(new Edge(this, piece, this.rand));
  }

  // displays this gamepiece
  WorldImage draw(int distToStation, int radius) {
    Color color = Color.GRAY;
    if (this.itsLit) {
      int alpha = 255 - (int) ((double) (distToStation) / (double) (radius) * 255);
      if (alpha > 25) {
        color = new Color(255, 255, 0, alpha);
      }
      else {
        color = new Color(255, 255, 0, 25);
      }
    }
    WorldImage img = new RectangleImage(50, 50, "solid", Color.DARK_GRAY);
    img = new OverlayImage(new RectangleImage(50, 50, "outline", Color.BLACK), img);
    if (this.left) {
      img = new OverlayImage(new RectangleImage(25, 5, "solid", color).movePinhole(12, 0), img);
    }
    if (this.top) {
      img = new OverlayImage(new RectangleImage(5, 25, "solid", color).movePinhole(0, 12), img);
    }
    if (this.right) {
      img = new OverlayImage(new RectangleImage(25, 5, "solid", color).movePinhole(-12, 0), img);
    }
    if (this.bottom) {
      img = new OverlayImage(new RectangleImage(5, 25, "solid", color).movePinhole(0, -12), img);
    }
    if (this.powerStation) {
      img = new OverlayImage(new StarImage(20, 7, OutlineMode.SOLID, Color.ORANGE), img);
      img = new OverlayImage(new StarImage(20, 7, OutlineMode.OUTLINE, Color.RED), img);
    }
    return img;
  }
}

// represents an edge on the graph of pieces in this game
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  Edge(GamePiece fromNode, GamePiece toNode, Random rand) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = rand.nextInt(1000);
  }

  // convenience constructor for testing
  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }

  // returns the weight of this edge as a string
  public String toString() {
    return "" + this.weight;
  }
}

class ExamplesPowerSupply {
  Random testRand;
  GamePiece p1;
  GamePiece p2;
  GamePiece p3;
  GamePiece p4;
  GamePiece p5;
  GamePiece p6;
  GamePiece p7;
  GamePiece p8;
  GamePiece p9;
  GamePiece p1a;
  GamePiece p2a;
  GamePiece p3a;
  GamePiece p4a;
  GamePiece p5a;
  GamePiece p6a;
  GamePiece p7a;
  GamePiece p8a;
  GamePiece p9a;
  ArrayList<GamePiece> nodes1;
  ArrayList<GamePiece> nodes2;
  ArrayList<GamePiece> nodes3;
  ArrayList<GamePiece> nodes4;
  ArrayList<GamePiece> nodes5;
  ArrayList<GamePiece> nodes6;
  ArrayList<GamePiece> nodes7;
  ArrayList<ArrayList<GamePiece>> board1;
  ArrayList<ArrayList<GamePiece>> board2;
  Edge e1;
  Edge e2;
  Edge e3;
  Edge e4;
  Edge e5;
  Edge e6;
  Edge e7;
  Edge e8;
  Edge e9;
  Edge e10;
  Edge e11;
  Edge e12;
  Edge e13;
  Edge e14;
  Edge e15;
  Edge e16;
  Edge e17;
  Edge e18;
  Edge e19;
  Edge e20;
  Edge e21;
  Edge e22;
  Edge e23;
  Edge e24;
  Edge e25;
  Edge e26;
  Edge e27;
  Edge e28;
  Edge e29;
  ArrayList<Edge> edges1;
  ArrayList<Edge> edges2;
  ArrayList<Edge> edges3;
  ArrayList<Edge> edges4;
  ArrayList<Edge> edges3Heap;
  ArrayList<Edge> mst1;
  HashMap<GamePiece, GamePiece> map1;
  HashMap<GamePiece, GamePiece> map2;
  PowerSupply game1;
  PowerSupply game2;
  WorldImage i1;
  WorldImage i2;
  WorldImage i3;
  WorldImage i4;
  WorldScene scene1;
  WorldScene scene2;

  void init() {
    this.testRand = new Random(75);
    this.p1 = new GamePiece(0, 0, false, true, false, true, false, this.testRand);
    this.p2 = new GamePiece(1, 0, false, true, true, true, false, this.testRand);
    this.p3 = new GamePiece(2, 0, false, true, false, true, false, this.testRand);
    this.p4 = new GamePiece(0, 1, false, true, false, true, false, this.testRand);
    this.p5 = new GamePiece(1, 1, true, true, true, true, true, this.testRand);
    this.p6 = new GamePiece(2, 1, false, true, false, true, false, this.testRand);
    this.p7 = new GamePiece(0, 2, false, true, false, true, false, this.testRand);
    this.p8 = new GamePiece(1, 2, true, true, false, true, false, this.testRand);
    this.p9 = new GamePiece(2, 2, false, true, false, true, false, this.testRand);
    this.p1a = new GamePiece(0, 0, false, false, false, false, true, this.testRand);
    this.p2a = new GamePiece(1, 0, false, false, false, false, false, this.testRand);
    this.p3a = new GamePiece(2, 0, false, false, false, false, false, this.testRand);
    this.p4a = new GamePiece(0, 1, false, false, false, false, false, this.testRand);
    this.p5a = new GamePiece(1, 1, false, false, false, false, false, this.testRand);
    this.p6a = new GamePiece(2, 1, false, false, false, false, false, this.testRand);
    this.p7a = new GamePiece(0, 2, false, false, false, false, false, this.testRand);
    this.p8a = new GamePiece(1, 2, false, false, false, false, false, this.testRand);
    this.p9a = new GamePiece(2, 2, false, false, false, false, false, this.testRand);
    this.nodes1 = new ArrayList<GamePiece>();
    this.nodes1.add(this.p1);
    this.nodes1.add(this.p2);
    this.nodes1.add(this.p3);
    this.nodes2 = new ArrayList<GamePiece>();
    this.nodes2.add(this.p4);
    this.nodes2.add(this.p5);
    this.nodes2.add(this.p6);
    this.nodes3 = new ArrayList<GamePiece>();
    this.nodes3.add(this.p7);
    this.nodes3.add(this.p8);
    this.nodes3.add(this.p9);
    this.nodes4 = new ArrayList<GamePiece>();
    this.nodes4.add(this.p1);
    this.nodes4.add(this.p2);
    this.nodes4.add(this.p3);
    this.nodes4.add(this.p4);
    this.nodes4.add(this.p5);
    this.nodes4.add(this.p6);
    this.nodes4.add(this.p7);
    this.nodes4.add(this.p8);
    this.nodes4.add(this.p9);
    this.nodes5 = new ArrayList<GamePiece>();
    this.nodes5.add(this.p1a);
    this.nodes5.add(this.p2a);
    this.nodes5.add(this.p3a);
    this.nodes6 = new ArrayList<GamePiece>();
    this.nodes6.add(this.p4a);
    this.nodes6.add(this.p5a);
    this.nodes6.add(this.p6a);
    this.nodes7 = new ArrayList<GamePiece>();
    this.nodes7.add(this.p7a);
    this.nodes7.add(this.p8a);
    this.nodes7.add(this.p9a);
    this.board1 = new ArrayList<ArrayList<GamePiece>>();
    this.board1.add(this.nodes1);
    this.board1.add(this.nodes2);
    this.board1.add(this.nodes3);
    this.board2 = new ArrayList<ArrayList<GamePiece>>();
    this.board2.add(this.nodes5);
    this.board2.add(this.nodes6);
    this.board2.add(this.nodes7);
    this.e1 = new Edge(this.p5, this.p2, 886);
    this.e2 = new Edge(this.p5, this.p4, 700);
    this.e3 = new Edge(this.p5, this.p8, 805);
    this.e4 = new Edge(this.p5, this.p6, 23);
    this.e5 = new Edge(this.p7, this.p8, 469);
    this.e6 = new Edge(this.p1, this.p4, 774);
    this.e7 = new Edge(this.p1, this.p2, 82);
    this.e8 = new Edge(this.p2, this.p1, 375);
    this.e9 = new Edge(this.p2, this.p5, 992);
    this.e10 = new Edge(this.p2, this.p3, 433);
    this.e11 = new Edge(this.p3, this.p2, 446);
    this.e12 = new Edge(this.p3, this.p6, 156);
    this.e13 = new Edge(this.p4, this.p1, 146);
    this.e14 = new Edge(this.p4, this.p7, 316);
    this.e15 = new Edge(this.p4, this.p5, 513);
    this.e16 = new Edge(this.p6, this.p3, 171);
    this.e17 = new Edge(this.p6, this.p5, 974);
    this.e18 = new Edge(this.p6, this.p9, 935);
    this.e19 = new Edge(this.p7, this.p4, 476);
    this.e20 = new Edge(this.p8, this.p5, 754);
    this.e21 = new Edge(this.p8, this.p7, 492);
    this.e22 = new Edge(this.p8, this.p9, 210);
    this.e23 = new Edge(this.p9, this.p6, 644);
    this.e24 = new Edge(this.p9, this.p8, 471);
    this.e25 = new Edge(this.p1, this.p2, 774);
    this.e26 = new Edge(this.p7, this.p8, 700);
    this.e27 = new Edge(this.p2, this.p1, 82);
    this.e28 = new Edge(this.p2, this.p5, 375);
    this.e29 = new Edge(this.p2, this.p3, 992);
    this.edges1 = new ArrayList<Edge>();
    this.edges1.add(this.e27);
    this.edges1.add(this.e28);
    this.edges1.add(this.e29);
    this.edges2 = new ArrayList<Edge>();
    this.edges2.add(this.e26);
    this.edges3 = new ArrayList<Edge>();
    this.edges3.add(this.e6);
    this.edges3.add(this.e7);
    this.edges3.add(this.e8);
    this.edges3.add(this.e9);
    this.edges3.add(this.e10);
    this.edges3.add(this.e11);
    this.edges3.add(this.e12);
    this.edges3.add(this.e13);
    this.edges3.add(this.e14);
    this.edges3.add(this.e15);
    this.edges3.add(this.e1);
    this.edges3.add(this.e2);
    this.edges3.add(this.e3);
    this.edges3.add(this.e4);
    this.edges3.add(this.e16);
    this.edges3.add(this.e17);
    this.edges3.add(this.e18);
    this.edges3.add(this.e19);
    this.edges3.add(this.e5);
    this.edges3.add(this.e20);
    this.edges3.add(this.e21);
    this.edges3.add(this.e22);
    this.edges3.add(this.e23);
    this.edges3.add(this.e24);
    this.edges4 = new ArrayList<Edge>();
    this.edges4.add(this.e25);
    this.mst1 = new ArrayList<Edge>();
    this.mst1.add(this.e4);
    this.mst1.add(this.e7);
    this.mst1.add(this.e13);
    this.mst1.add(this.e12);
    this.mst1.add(this.e22);
    this.mst1.add(this.e14);
    this.mst1.add(this.e10);
    this.mst1.add(this.e5);
    this.map1 = new HashMap<GamePiece, GamePiece>();
    this.map1.put(this.p1, this.p1);
    this.map1.put(this.p2, this.p3);
    this.map1.put(this.p3, this.p3);
    this.map2 = new HashMap<GamePiece, GamePiece>();
    this.map2.put(this.p5, this.p6);
    this.map2.put(this.p6, this.p6);
    this.map2.put(this.p2, this.p5);
    this.map2.put(this.p4, this.p5);
    this.game1 = new PowerSupply(8, 8); // EDIT SIZE OF MAIN GAME HERE
    this.game2 = new PowerSupply(3, 3, this.board1, this.nodes4, this.edges3, this.testRand);
    this.i1 = new OverlayImage(new RectangleImage(5, 25, "solid", Color.GRAY).movePinhole(0, -12),
        new OverlayImage(new RectangleImage(5, 25, "solid", Color.GRAY).movePinhole(0, 12),
            new OverlayImage(new RectangleImage(50, 50, "outline", Color.BLACK),
                new RectangleImage(50, 50, "solid", Color.DARK_GRAY)))); // p1
    this.i2 = new OverlayImage(new RectangleImage(5, 25, "solid", Color.GRAY).movePinhole(0, -12),
        new OverlayImage(new RectangleImage(25, 5, "solid", Color.GRAY).movePinhole(-12, 0),
            new OverlayImage(new RectangleImage(5, 25, "solid", Color.GRAY).movePinhole(0, 12),
                new OverlayImage(new RectangleImage(50, 50, "outline", Color.BLACK),
                    new RectangleImage(50, 50, "solid", Color.DARK_GRAY))))); // p2
    this.i3 = new OverlayImage(new StarImage(20, 7, OutlineMode.OUTLINE, Color.RED),
        new OverlayImage(new StarImage(20, 7, OutlineMode.SOLID, Color.ORANGE), new OverlayImage(
            new RectangleImage(5, 25, "solid", Color.GRAY).movePinhole(0, -12),
            new OverlayImage(new RectangleImage(25, 5, "solid", Color.GRAY).movePinhole(-12, 0),
                new OverlayImage(new RectangleImage(5, 25, "solid", Color.GRAY).movePinhole(0, 12),
                    new OverlayImage(
                        new RectangleImage(25, 5, "solid", Color.GRAY).movePinhole(12, 0),
                        new OverlayImage(new RectangleImage(50, 50, "outline", Color.BLACK),
                            new RectangleImage(50, 50, "solid", Color.DARK_GRAY)))))))); // p5

    this.scene1 = this.game2.getEmptyScene();
    this.scene1.placeImageXY(this.i1, 0, 0);
    this.scene1.placeImageXY(this.i2, 0, 50);
    this.scene1.placeImageXY(
        this.p3.draw(this.game2.findDistance(this.p3, this.p5), this.game2.radius), 0, 100);
    this.scene1.placeImageXY(
        this.p4.draw(this.game2.findDistance(this.p4, this.p5), this.game2.radius), 50, 0);
    this.scene1.placeImageXY(this.i3, 50, 50);
    this.scene1.placeImageXY(
        this.p6.draw(this.game2.findDistance(this.p6, this.p5), this.game2.radius), 50, 100);
    this.scene1.placeImageXY(
        this.p7.draw(this.game2.findDistance(this.p7, this.p5), this.game2.radius), 100, 0);
    this.scene1.placeImageXY(
        this.p8.draw(this.game2.findDistance(this.p8, this.p5), this.game2.radius), 100, 50);
    this.scene1.placeImageXY(
        this.p9.draw(this.game2.findDistance(this.p9, this.p5), this.game2.radius), 100, 100);
    this.scene2 = this.game2.getEmptyScene();
    this.scene2.placeImageXY(this.i1, 0, 0);
    this.scene2.placeImageXY(this.i2, 0, 50);
    this.scene2.placeImageXY(
        this.p3.draw(this.game2.findDistance(this.p3, this.p5), this.game2.radius), 0, 100);
    this.scene2.placeImageXY(
        this.p4.draw(this.game2.findDistance(this.p4, this.p5), this.game2.radius), 50, 0);
    this.scene2.placeImageXY(this.i3, 50, 50);
    this.scene2.placeImageXY(
        this.p6.draw(this.game2.findDistance(this.p6, this.p5), this.game2.radius), 50, 100);
    this.scene2.placeImageXY(
        this.p7.draw(this.game2.findDistance(this.p7, this.p5), this.game2.radius), 100, 0);
    this.scene2.placeImageXY(
        this.p8.draw(this.game2.findDistance(this.p8, this.p5), this.game2.radius), 100, 50);
    this.scene2.placeImageXY(
        this.p9.draw(this.game2.findDistance(this.p9, this.p5), this.game2.radius), 100, 100);
    this.scene2.placeImageXY(new TextImage("YOU WIN!", 60, Color.GREEN), this.scene2.width / 2,
        this.scene2.height / 2);
  }

  void testBigBang(Tester t) {
    this.init();
    PowerSupply game = this.game1;
    game.bigBang(game.width * 50, game.height * 50, 1.0);
  }

  void testOnTick(Tester t) {
    this.init();
    t.checkExpect(this.game1.tickNum, 0); // BEFORE CHANGE
    this.game1.onTick(); // CHANGE 1
    t.checkExpect(this.game1.tickNum, 1); // AFTER CHANGE 1
    this.game1.onTick(); // CHANGE 2
    t.checkExpect(this.game1.tickNum, 2); // AFTER CHANGE 2
  }

  void testDummyBoard(Tester t) {
    this.init();
    t.checkExpect(this.game2.dummyBoard(), this.board2);
  }

  void testSetNodes(Tester t) {
    this.init();
    t.checkExpect(this.game2.setNodes(), this.nodes4);
  }

  void testSetEdges(Tester t) {
    this.init();
    t.checkExpect(this.game2.setEdges(), this.edges3);
  }

  void testSetMST(Tester t) {
    this.init();
    t.checkExpect(this.game2.setMST(), this.mst1);
  }

  void testMultipleSelfReps(Tester t) {
    this.init();
    t.checkExpect(this.game2.multipleSelfReps(this.map1), true);
    t.checkExpect(this.game2.multipleSelfReps(this.map2), false);
  }

  void testFindRep(Tester t) {
    this.init();
    t.checkExpect(this.game2.findRep(this.map1, this.p3), this.p3);
    t.checkExpect(this.game2.findRep(this.map2, this.p5), this.p6);
    t.checkExpect(this.game2.findRep(this.map2, this.p4), this.p6);
    t.checkExpect(this.game2.findRep(this.map2, this.p6), this.p6);
  }

  void testHeapsortEdges(Tester t) {
    this.init();
    t.checkExpect(this.game2.heapsortEdges().toString(),
        "[23, 82, 146, 156, 171, 210, 316, 375, 433, 446, 469, 471, 476, 492, 513, 644, 700, 754,"
            + " 774, 805, 886, 935, 974, 992]");
  }

  void testSetConnections(Tester t) {
    this.init();

    // set up board and MST
    this.game2.board = this.game2.dummyBoard();
    this.game2.nodes = this.game2.setNodes();
    this.game2.edges = this.game2.setEdges();
    this.game2.mst = this.game2.setMST();

    // BEFORE
    t.checkExpect(this.game2.board.get(1).get(1).bottom, false);
    t.checkExpect(this.game2.board.get(1).get(2).top, false);
    // CHANGE
    this.game2.setConnections();
    // AFTER
    t.checkExpect(this.game2.board.get(1).get(1).bottom, true);
    t.checkExpect(this.game2.board.get(1).get(2).top, true);
  }

  void testScrambleBoard(Tester t) {
    this.init();
    // BEFORE
    t.checkExpect(this.game2.board.get(1).get(1).bottom, true);
    t.checkExpect(this.game2.board.get(1).get(1).left, true);
    t.checkExpect(this.game2.board.get(2).get(2).right, false);
    t.checkExpect(this.game2.board.get(0).get(2).bottom, true);
    // CHANGE
    this.game2.scrambleBoard();
    // AFTER
    t.checkExpect(this.game2.board.get(1).get(1).bottom, true);
    t.checkExpect(this.game2.board.get(1).get(1).left, true);
    t.checkExpect(this.game2.board.get(2).get(2).right, true);
    t.checkExpect(this.game2.board.get(0).get(2).bottom, false);
  }

  void testFindRadius(Tester t) {
    this.init();
    // give gamepieces edges for the findRadius helpers to iterate through
    this.game2.updatePieceEdges();

    // test
    t.checkExpect(this.game2.findRadius(), 3);
  }

  void testFurthestFrom(Tester t) {
    this.init();

    // give gamepieces edges to iterate through
    this.game2.updatePieceEdges();

    // small-scale test
    t.checkExpect(this.game2.furthestFrom(this.game2.board.get(0).get(0)),
        this.game2.board.get(2).get(2));

    // node is furthest from itself when disconnected from the rest of the graph:
    this.game2.board.get(1).get(2).rotate();
    this.game2.updatePieceEdges();
    t.checkExpect(this.game2.furthestFrom(this.game2.board.get(1).get(2)),
        this.game2.board.get(1).get(2));
  }

  void testFindDistance(Tester t) {
    this.init();

    // give gamepieces edges to iterate through
    this.game2.updatePieceEdges();

    // test
    t.checkExpect(
        this.game2.findDistance(this.game2.board.get(0).get(0), this.game2.board.get(1).get(0)), 3);
    t.checkExpect(
        this.game2.findDistance(this.game2.board.get(2).get(0), this.game2.board.get(0).get(2)), 4);
    // pieces are not connected:
    t.checkExpect(this.game1.findDistance(this.game1.board.get(3).get(5), this.p3), -1);
  }

  void testSendPower(Tester t) {
    this.init();

    // BEFORE 1
    t.checkExpect(this.game2.board.get(0).get(0).itsLit, false);
    t.checkExpect(this.game2.board.get(0).get(1).itsLit, false);
    t.checkExpect(this.game2.board.get(2).get(2).itsLit, false);
    // CHANGE 1
    this.game2.sendPower(this.game2.powerCol, this.game2.powerRow, this.game2.radius);
    // AFTER 1
    t.checkExpect(this.game2.board.get(0).get(0).itsLit, true);
    t.checkExpect(this.game2.board.get(0).get(1).itsLit, true);
    t.checkExpect(this.game2.board.get(2).get(2).itsLit, false);
    // BEFORE 2
    t.checkExpect(this.game2.board.get(2).get(1).itsLit, false);
    // CHANGE 2
    this.game2.onKeyEvent("down"); // move power station
    this.game2.sendPower(this.game2.powerCol, this.game2.powerRow, this.game2.radius);
    // AFTER 2 / BEFORE 3
    t.checkExpect(this.game2.board.get(2).get(1).itsLit, true);
    // CHANGE 3
    this.game2.board.get(0).get(1).rotate();
    this.game2.board.get(0).get(1).rotate(); // cut off pipe access
    this.game2.sendPower(this.game2.powerCol, this.game2.powerRow, this.game2.radius);
    // AFTER 3
    t.checkExpect(this.game2.board.get(2).get(1).itsLit, false);
  }

  void testUpdatePieceEdges(Tester t) {
    this.init();
    // BEFORE
    t.checkExpect(this.game2.board.get(0).get(1).edges, new ArrayList<Edge>());
    t.checkExpect(this.game2.board.get(2).get(0).edges, new ArrayList<Edge>());
    // CHANGE
    this.game2.updatePieceEdges();
    // AFTER
    t.checkExpect(this.game2.board.get(0).get(1).edges, this.edges1);
    t.checkExpect(this.game2.board.get(2).get(0).edges, this.edges2);
  }

  void testGameWon(Tester t) {
    this.init();

    // send power to allow for potential win
    this.game2.sendPower(this.game2.powerCol, this.game2.powerRow, this.game2.radius);

    // test
    t.checkExpect(this.game1.gameWon(), false);
    t.checkExpect(this.game2.gameWon(), false);
    // move power station to winning position:
    this.game2.powerCol = 1;
    this.game2.powerRow = 1;
    this.game2.sendPower(this.game2.powerCol, this.game2.powerRow, this.game2.radius);
    t.checkExpect(this.game2.gameWon(), true);
  }

  void testMakeScene(Tester t) {
    this.init();
    t.checkExpect(this.game2.makeScene(), this.scene1);
  }

  void testLastScene(Tester t) {
    this.init();
    t.checkExpect(this.game2.lastScene("YOU WIN!"), this.scene2);
  }

  void testOnMouseClicked(Tester t) {
    this.init();
    // BEFORE
    t.checkExpect(this.game2.board.get(0).get(0).left, false);
    t.checkExpect(this.game2.board.get(0).get(0).top, true);
    t.checkExpect(this.game2.board.get(2).get(1).right, false);
    // CHANGE
    this.game2.onMouseClicked(new Posn(27, 40));
    this.game2.onMouseClicked(new Posn(133, 52));
    this.game2.onMouseClicked(new Posn(1000, 1000)); // out of bounds, no effect
    // AFTER
    t.checkExpect(this.game2.board.get(0).get(0).left, true);
    t.checkExpect(this.game2.board.get(0).get(0).top, false);
    t.checkExpect(this.game2.board.get(2).get(1).right, true);
  }

  void testOnKeyEvent(Tester t) {
    this.init();
    // don't want game/world to end while testing, so don't power pieces:
    this.game2.radius = 0;
    // BEFORE 1
    t.checkExpect(this.game2.powerRow, 0);
    // CHANGE 1
    this.game2.onKeyEvent("up");
    // AFTER 1 / BEFORE 2
    t.checkExpect(this.game2.powerRow, 0); // no change because already on top row
    // CHANGE 2
    this.game2.onKeyEvent("down");
    // AFTER 2
    t.checkExpect(this.game2.powerRow, 1);
    // BEFORE 3
    t.checkExpect(this.game2.powerCol, 0);
    // CHANGE 3
    this.game2.onKeyEvent("left");
    // AFTER 3 / BEFORE 4
    t.checkExpect(this.game2.powerCol, 0); // no change because already on leftmost column
    // CHANGE 4
    this.game2.onKeyEvent("right");
    // AFTER 3
    t.checkExpect(this.game2.powerCol, 1);
  }

  void testRotate(Tester t) {
    this.init();
    // BEFORE
    t.checkExpect(this.p1, new GamePiece(0, 0, false, true, false, true, false, this.testRand));
    t.checkExpect(this.p2, new GamePiece(1, 0, false, true, true, true, false, this.testRand));
    t.checkExpect(this.p5, new GamePiece(1, 1, true, true, true, true, true, this.testRand));
    t.checkExpect(this.p8, new GamePiece(1, 2, true, true, false, true, false, this.testRand));
    // CHANGE
    this.p1.rotate();
    this.p2.rotate();
    this.p5.rotate();
    this.p8.rotate();
    // AFTER
    t.checkExpect(this.p1, new GamePiece(0, 0, true, false, true, false, false, this.testRand));
    t.checkExpect(this.p2, new GamePiece(1, 0, true, false, true, true, false, this.testRand));
    t.checkExpect(this.p5, new GamePiece(1, 1, true, true, true, true, true, this.testRand));
    t.checkExpect(this.p8, new GamePiece(1, 2, true, true, true, false, false, this.testRand));
  }

  void testAddEdge(Tester t) {
    this.init();
    // BEFORE 1
    t.checkExpect(this.p1.edges, new ArrayList<Edge>());
    // CHANGE 1
    this.p1.addEdge(this.p2);
    // AFTER 1
    t.checkExpect(this.p1.edges, this.edges4);
  }

  void testDraw(Tester t) {
    this.init();
    t.checkExpect(this.p1.draw(this.game2.findDistance(this.p1, this.p5), this.game2.radius),
        this.i1);
    t.checkExpect(this.p2.draw(this.game2.findDistance(this.p2, this.p5), this.game2.radius),
        this.i2);
    t.checkExpect(this.p5.draw(this.game2.findDistance(this.p5, this.p5), this.game2.radius),
        this.i3);
  }

  void testEdgeToString(Tester t) {
    this.init();
    t.checkExpect(this.e2.toString(), "700");
    t.checkExpect(this.e24.toString(), "471");
  }

  void testExceptions(Tester t) {
    t.checkConstructorException(
        new IllegalArgumentException("Width and height must be positive values."), "LightEmAll", 0,
        10);
    t.checkConstructorException(
        new IllegalArgumentException("Width and height must be positive values."), "LightEmAll", 0,
        0);
    t.checkConstructorException(
        new IllegalArgumentException("Width and height must be positive values."), "LightEmAll", 6,
        0, this.board1, this.nodes1, this.edges1, this.testRand);
  }

}