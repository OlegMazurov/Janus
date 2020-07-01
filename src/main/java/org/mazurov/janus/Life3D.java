package org.mazurov.janus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;

public class Life3D extends Application {

    private static final boolean OPTIMIZED = true;

    /* Space dimensions, powers of 2 */
    private static final int X = 256, Y = 256, Z = 256;

    /* Initial GoL pattern */
    private final int[][] ACORN = {
            {1, 1, 0, 0, 1, 1, 1},
            {0, 0, 0, 1, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 0}
    };

    /* Half-length of the initial cube */
    private final int L = ACORN[0].length / 2 + 1;

    /* A long[] array keeps the entire 3D state. Each long value represents a 4x4x4 bit cube. */
    private long[] gen0;
    private long[] gen1;

    private int nThreads;
    private CyclicBarrier barrier;
    private int maxTime = (Z - 2 * L - 1) / 2;
    private int globalTime;
    private int localTime;
    private int dt;

    private int getState(long[] gen, int x, int y, int z) {
        x &= (X - 1);
        y &= (Y - 1);
        z &= (Z - 1);
        long v = gen[(z/4 * Y/4 + y/4) * X/4 + x/4];
        if (v == 0) return 0;
        return (int)((v >> ((z%4 * 4 + y%4) * 4 + x%4)) & 1);
    }

    private void updateState(long[] gen, int x, int y, int z) {
        x &= (X - 1);
        y &= (Y - 1);
        z &= (Z - 1);
        gen[(z/4 * Y/4 + y/4) * X/4 + x/4] ^= (long)1 << ((z%4 * 4 + y%4) * 4 + x%4);
    }

    private void runStaticSchedule(int id) {
        try {
            int minY = (id * Y / nThreads) & ~3;
            int maxY = ((id + 1) * Y / nThreads) & ~3;

            for (; ; ) {
                for (int z = 0; z < Z; ++z) {
                    for (int y = minY; y < maxY; ++y) {
                        for (int x = 0; x < X; ++x) {
                            int sum = 0;
                            int xx = 0, yy = 0, zz = 0;
                            if (!OPTIMIZED) {
                                for (int dz = -1; dz <= 1; ++dz) {
                                    for (int dy = -1; dy <= 1; ++dy) {
                                        for (int dx = -1; dx <= 1; ++dx) {
                                            int s = getState(gen0, x + dx, y + dy, z + dz);
                                            sum += s;
                                            if (s == 1) {
                                                xx += dx;
                                                yy += dy;
                                                zz += dz;
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                // Unroll the loop above
                                int s;
                                s = getState(gen0, x - 1, y - 1, z - 1); sum += s; xx -= s; yy -= s; zz -= s;
                                s = getState(gen0, x    , y - 1, z - 1); sum += s; yy -= s; zz -= s;
                                s = getState(gen0, x + 1, y - 1, z - 1); sum += s; xx += s; yy -= s; zz -= s;
                                s = getState(gen0, x - 1, y    , z - 1); sum += s; xx -= s; zz -= s;
                                s = getState(gen0, x    , y    , z - 1); sum += s; zz -= s;
                                s = getState(gen0, x + 1, y    , z - 1); sum += s; xx += s; zz -= s;
                                s = getState(gen0, x - 1, y + 1, z - 1); sum += s; xx -= s; yy += s; zz -= s;
                                s = getState(gen0, x    , y + 1, z - 1); sum += s; yy += s; zz -= s;
                                s = getState(gen0, x + 1, y + 1, z - 1); sum += s; xx += s; yy += s; zz -= s;
                                s = getState(gen0, x - 1, y - 1, z    ); sum += s; xx -= s; yy -= s;
                                s = getState(gen0, x    , y - 1, z    ); sum += s; yy -= s;
                                s = getState(gen0, x + 1, y - 1, z    ); sum += s; xx += s; yy -= s;
                                s = getState(gen0, x - 1, y    , z    ); sum += s; xx -= s;
                                s = getState(gen0, x    , y    , z    ); sum += s;
                                s = getState(gen0, x + 1, y    , z    ); sum += s; xx += s;
                                s = getState(gen0, x - 1, y + 1, z    ); sum += s; xx -= s; yy += s;
                                s = getState(gen0, x    , y + 1, z    ); sum += s; yy += s;
                                s = getState(gen0, x + 1, y + 1, z    ); sum += s; xx += s; yy += s;
                                s = getState(gen0, x - 1, y - 1, z + 1); sum += s; xx -= s; yy -= s; zz += s;
                                s = getState(gen0, x    , y - 1, z + 1); sum += s; yy -= s; zz += s;
                                s = getState(gen0, x + 1, y - 1, z + 1); sum += s; xx += s; yy -= s; zz += s;
                                s = getState(gen0, x - 1, y    , z + 1); sum += s; xx -= s; zz += s;
                                s = getState(gen0, x    , y    , z + 1); sum += s; zz += s;
                                s = getState(gen0, x + 1, y    , z + 1); sum += s; xx += s; zz += s;
                                s = getState(gen0, x - 1, y + 1, z + 1); sum += s; xx -= s; yy += s; zz += s;
                                s = getState(gen0, x    , y + 1, z + 1); sum += s; yy += s; zz += s;
                                s = getState(gen0, x + 1, y + 1, z + 1); sum += s; xx += s; yy += s; zz += s;
                            }

                            // The T-34 rule
                            int ss = 0;
                            if (sum == 3) {
                                ss = 1;
                            } else if (sum == 4) {
                                ss = getState(gen0, x + (int) (xx / 4.), y + (int) (yy / 4.), z + (int) (zz / 4.));
                            }
                            if (ss == 1) {
                                updateState(gen1, x, y, z);
                            }
                        }
                    }
                }
                barrier.await();
            }
        }
        catch (Throwable t) {
            System.err.println("ERROR in thread " + id);
            t.printStackTrace();
        }
    }

    private void initFace(long[] gen, int[] pt, int[] sx, int[] sy) {
        for (int y = 0; y < ACORN.length; ++y) {
            for (int x = 0; x < ACORN[y].length; ++x) {
                if (ACORN[y][x] != 0) {
                    updateState(gen,
                            pt[0] + x * sx[0] + y * sy[0],
                            pt[1] + x * sx[1] + y * sy[1],
                            pt[2] + x * sx[2] + y * sy[2]
                    );
                }
            }
        }
    }

    private void initState() {
        gen0 = new long[X/4 * Y/4 * Z/4];
        gen1 = new long[gen0.length];

        // Put the acorn pattern on the 6 faces of the initial cube
        int lx = ACORN[0].length / 2;
        int ly = ACORN.length / 2;
        initFace(gen0, new int[]{X/2 - L, Y/2 + ly, Z/2 + lx}, new int[]{ 0,  0, -1}, new int[]{0, -1, 0});
        initFace(gen0, new int[]{X/2 + L, Y/2 + ly, Z/2 - lx}, new int[]{ 0,  0,  1}, new int[]{0, -1, 0});
        initFace(gen0, new int[]{X/2 - lx, Y/2 - L, Z/2 - ly}, new int[]{ 1,  0,  0}, new int[]{0,  0, 1});
        initFace(gen0, new int[]{X/2 + lx, Y/2 + L, Z/2 - ly}, new int[]{-1,  0,  0}, new int[]{0,  0, 1});
        initFace(gen0, new int[]{X/2 - ly, Y/2 - lx, Z/2 - L}, new int[]{ 0,  1,  0}, new int[]{1,  0, 0});
        initFace(gen0, new int[]{X/2 - ly, Y/2 + lx, Z/2 + L}, new int[]{ 0, -1,  0}, new int[]{1,  0, 0});
        sceneData.update(gen0);
    }

    private void startExecution() {

        initState();

        // Prepare threads
        nThreads = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[nThreads];
        for (int t = 0; t < threads.length; ++t) {
            final int id = t;
            Thread thread = new Thread(() -> runStaticSchedule(id));
            thread.setDaemon(true);
            threads[t] = thread;
        }

        globalTime = 1;
        localTime = 1; dt = 1;
        barrier = new CyclicBarrier(nThreads, () -> {
            sceneData.update(gen1);
            if (localTime % maxTime != 0) {
                // Swap generations
                long[] t = gen0;
                gen0 = gen1;
                gen1 = t;
            }
            else {
                // Reverse time
                dt = - dt;
                localTime += dt;
            }
            localTime += dt;
            globalTime += 1;
        });

        for (Thread thread : threads) {
            thread.start();
        }
    }

    /*
     *  Graphics
     */

    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 600;
    private static final int DEFAULT_CELL_SIZE = 10;
    private static final int DEFAULT_VIZIBLE_CELLS = 10000;
    private static final PhongMaterial intMaterial = new PhongMaterial(Color.web("306530"));
    private static final PhongMaterial extMaterial = new PhongMaterial(Color.web("b81313"));

    private final SceneData sceneData;
    private Scene scene;
    private int cellSize = DEFAULT_CELL_SIZE;
    private boolean paused;
    private double mouseX;
    private double mouseY;
    private Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private volatile double z0 = 0.;
    private Translate translateZ = new Translate(0, 0, z0);
    private Text footer;
    private int imgCount = 1;

    private void reset() {
        rotateX.setAngle(-15.);
        rotateY.setAngle(-15.);
        z0 = -5500.;
    }

    private int computeMetric(int x, int y, int z) {
        int val = Math.abs(x - X/2);
        val = Math.max(val, Math.abs(y - Y/2));
        return Math.max(val, Math.abs(z - Z/2));
    }

    class SceneData {
        Cell[] cells;
        int gTime;
        int lTime;

        SceneData(int nCells) {
            cells = new Cell[nCells];
            for (int i = 0; i < cells.length; ++i) {
                Cell cell = new Cell(cellSize - 1, cellSize - 1, cellSize - 1);
                cells[i] = cell;
            }
        }

        // Emulate a priority queue to collect cells with highest metric values
        void insert(int x, int y, int z) {
            int curMetric = computeMetric(x, y, z);
            if (curMetric < cells[0].getMetric()) return;
            int curIdx = 0;
            Cell result = cells[curIdx];
            for (;;) {
                int nextIdx = 2 * curIdx + 1;
                if (nextIdx >= cells.length) break;
                int nextMetric = cells[nextIdx].getMetric();
                if (nextIdx + 1 < cells.length) {
                    int val = cells[nextIdx + 1].getMetric();
                    if (val < nextMetric) {
                        nextIdx = nextIdx + 1;
                        nextMetric = val;
                    }
                }
                if (curMetric <= nextMetric) break;
                cells[curIdx] = cells[nextIdx];
                curIdx = nextIdx;
            }
            cells[curIdx] = result;
            result.x = x;
            result.y = y;
            result.z = z;
            result.visible = true;
            result.external = curMetric == L + lTime;
        }

        void update(long[] gen) {
            synchronized (this) {
                gTime = globalTime;
                lTime = localTime;

                for (Cell cell : cells) {
                    cell.visible = false;
                }
                for (int z = 0; z < Z / 4; ++z) {
                    for (int y = 0; y < Y / 4; ++y) {
                        for (int x = 0; x < X / 4; ++x) {
                            long v = gen[(z * Y / 4 + y) * X / 4 + x];
                            if (v == 0) continue;
                            for (int zz = 0; zz < 4; ++zz) {
                                for (int yy = 0; yy < 4; ++yy) {
                                    for (int xx = 0; xx < 4; ++xx) {
                                        if ((v & 1) != 0) {
                                            insert(x * 4 + xx, y * 4 + yy, z * 4 + zz);
                                        }
                                        v >>>= 1;
                                    }
                                }
                            }
                        }
                    }
                }

                if (paused) {
                    try {
                        wait();
                    }
                    catch(InterruptedException ex) {}
                }
            }

            // Submit for rendering
            Platform.runLater(() -> {
                synchronized (SceneData.this) {
                    for (Cell cell : cells) {
                        cell.updateCell();
                    }
                    footer.setText("Time: " + gTime + " Generation: " + lTime);
                    translateZ.setZ(z0 - (lTime + L) * cellSize);
                }
            });
        }
    }

    class Cell extends Box {
        int x, y, z;
        boolean external;
        boolean visible;

        Cell(int lx, int ly, int lz) {
            super(lx, ly, lz);
        }

        void updateCell() {
            if (visible) {
                setTranslateX(x * cellSize);
                setTranslateY(y * cellSize);
                setTranslateZ(z * cellSize);
                setMaterial(external ? extMaterial : intMaterial);
            }
            setVisible(visible);
        }

        int getMetric() {
            if (!visible) return -1;
            return computeMetric(x, y, z);
        }
    }

    @Override
    public void start(Stage stage) {

        reset();

        // 3D
        Group root3D = new Group();
        root3D.getChildren().addAll(sceneData.cells);

        SubScene subScene = new SubScene(root3D, DEFAULT_WIDTH, DEFAULT_HEIGHT, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.grayRgb(40));

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setVerticalFieldOfView(false);
        camera.setTranslateX(X / 2 * cellSize);
        camera.setTranslateY(Y / 2 * cellSize);
        camera.setTranslateZ(Z / 2 * cellSize);
        camera.getTransforms().addAll(rotateX, rotateY, translateZ);
        subScene.setCamera(camera);

        PointLight light = new PointLight(Color.GAINSBORO);
        light.setTranslateZ(-10000.);
        root3D.getChildren().add(light);
        root3D.getChildren().add(new AmbientLight(Color.WHITE));

        subScene.setOnMousePressed((MouseEvent me) -> {
            mouseX = me.getSceneX();
            mouseY = me.getSceneY();
        });

        subScene.setOnMouseDragged((MouseEvent me) -> {
            double x = me.getSceneX();
            double y = me.getSceneY();
            rotateX.setAngle(rotateX.getAngle() - (y - mouseY));
            rotateY.setAngle(rotateY.getAngle() + (x - mouseX));
            mouseX = x;
            mouseY = y;
        });

        subScene.setOnScroll((ScrollEvent se) -> {
            double newZ = translateZ.getZ() + se.getDeltaY() + (localTime + L) * cellSize;
            if (newZ < -50000. || newZ > -100.) return;
            z0 = newZ;
            translateZ.setZ(z0 - (localTime + L) * cellSize);
        });

        // 2D
        footer = new Text("");
        scene = new Scene(new VBox(subScene, footer));
        scene.setFill(Color.grayRgb(160));

        scene.setOnKeyTyped((KeyEvent ke) -> {
            switch (ke.getCharacter()) {
                case " ":
                    synchronized (sceneData) {
                        paused = !paused;
                        if (!paused) {
                            sceneData.notify();
                        }
                    }
                    break;
                case "q":
                    System.exit(0);
                    break;
                case "r":
                    reset();
                    break;
                case "s":
                    WritableImage img = new WritableImage((int)scene.getWidth(), (int)scene.getHeight());
                    scene.snapshot(img);
                    try {
                        javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new java.io.File("image" + (imgCount++) + ".png"));
                    }
                    catch (IOException ex) {
                        System.out.println("ERROR: " + ex);
                    }
                    break;
            }
        });

        // Resize
        scene.widthProperty().addListener((observable, oldValue, newValue) ->
            subScene.setWidth(newValue.doubleValue())
        );
        scene.heightProperty().addListener((observable, oldValue, newValue) ->
            subScene.setHeight(newValue.doubleValue() - 20.)
        );

        stage.setTitle("Time-reversible Anisotropic Game of Life");
        stage.setScene(scene);
        stage.show();

        startExecution();
    }

    public Life3D() {
        sceneData = new SceneData(DEFAULT_VIZIBLE_CELLS);
    }

    public static void main(String[] args) {
        launch(args);
    }
}