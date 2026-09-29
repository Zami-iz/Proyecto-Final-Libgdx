package tile;

import java.util.ArrayDeque;

public final class MapGenerator {

    // Índices de tiles de suelo
    public static final int TILE_GRASS1 = 0;
    public static final int TILE_GRASS2 = 1;
    public static final int TILE_GRASS3 = 2;
    public static final int TILE_GRASS4 = 3;
    public static final int TILE_STONE  = 4;
    public static final int TILE_WATER0 = 5;
    public static final int TILE_SAND1  = 9;
    public static final int TILE_SAND2  = 10;
    public static final int TILE_DIRT1  = 11;

    // Índices de decoraciones (van en mapTreeNum, no en mapTileNum)
    public static final int TILE_TREE1  = 12;
    public static final int TILE_TREE2  = 13;
    public static final int TILE_TREE3  = 14;
    public static final int TILE_TREE4  = 15;

    // Pinos del bioma nieve
    public static final int TILE_PINE1  = 16;
    public static final int TILE_PINE2  = 17;
    public static final int TILE_PINE3  = 18;
    public static final int TILE_PINE4  = 19;
    public static final int TILE_PINE5  = 20;

    public static final int TILE_PALM1  = 21;
    public static final int TILE_PALM2  = 22;

    // Umbrales de elevación
    public static final double DEEP_WATER_THRESHOLD    = 0.30;
    public static final double SHALLOW_WATER_THRESHOLD = 0.38;
    // La arena va de SHALLOW_WATER_THRESHOLD a SAND_THRESHOLD
    public static final double SAND_THRESHOLD          = 0.42;
    public static final double GRASS_THRESHOLD         = 0.78;  // a partir de acá, piedra

    // Umbrales de bioma
    public static final double SNOW_THRESHOLD   = 0.28;
    public static final double SNOW_BLEND_EDGE  = 0.32;
    public static final double FOREST_THRESHOLD = 0.60;
    public static final double FOREST_BLEND_LOW = 0.50;

    // Ancho máximo de la franja de arena medido desde el agua más cercana
    private static final int MAX_SAND_WIDTH = 7;

    private final int  mapCols, mapRows;
    private final long seed;
    private final boolean isSnowyDay;
    private final java.util.Random rand;

    private double[][] grassVarCache;

    // Salida
    public int[][]     mapTileNum;
    public int[][]     mapTreeNum;
    public boolean[][] isShallowWater;
    public boolean[][] isDeepWater;
    public boolean[][] isSnowBiome;
    public boolean[][] isBeachBiome;
    public float[][]   snowBlend;
    public float[][]   deepWaterBlend;
    public int[][]     mapTransType;
    public int[][]     mapTransRot;

    private boolean[][] riverWater;
    private boolean[][] riverSand;

    public MapGenerator(int cols, int rows, long seed, String seedString) {
        this.mapCols    = cols;
        this.mapRows    = rows;
        this.seed       = seed;
        this.isSnowyDay = (seedString != null && seedString.equalsIgnoreCase("snowy day"));
        this.rand       = new java.util.Random(seed);

        mapTileNum     = new int[cols][rows];
        mapTreeNum     = new int[cols][rows];
        isShallowWater = new boolean[cols][rows];
        isDeepWater    = new boolean[cols][rows];
        isSnowBiome    = new boolean[cols][rows];
        isBeachBiome   = new boolean[cols][rows];
        snowBlend      = new float[cols][rows];
        deepWaterBlend = new float[cols][rows];
        mapTransType   = new int[cols][rows];
        mapTransRot    = new int[cols][rows];
        grassVarCache  = new double[cols][rows];
    }

    public void generate() {
        PerlinNoise elevNoise     = new PerlinNoise(seed);
        PerlinNoise detailNoise   = new PerlinNoise(seed + 1337);
        PerlinNoise tempNoise     = new PerlinNoise(seed + 7777);
        PerlinNoise forestNoise   = new PerlinNoise(seed + 3141);
        PerlinNoise dirtNoise     = new PerlinNoise(seed + 9999);
        PerlinNoise clearNoise    = new PerlinNoise(seed + 2718);
        PerlinNoise grassVarNoise = new PerlinNoise(seed + 6543);
        PerlinNoise lakeNoise     = new PerlinNoise(seed + 5555);

        double scaleElev     = 0.005;
        double scaleDetail   = 0.025;
        double scaleForest   = 0.009;
        double scaleDirt     = 0.055;
        double scaleClear    = 0.040;
        double scaleGrassVar = 0.080;

        // Un solo bioma nevado grande, garantizado dentro del mapa
        java.util.Random biomeRand = new java.util.Random(seed + 98765L);
        int snowCx = 150 + biomeRand.nextInt(mapCols - 300);
        int snowCy = 150 + biomeRand.nextInt(mapRows - 300);
        double maxSnowRadius = 80 + biomeRand.nextDouble() * 40;

        // Playas cerca de los bordes del mapa
        java.util.Random beachRand  = new java.util.Random(seed + 12345L);
        int numBeaches    = 3;
        int[][] beachCenters = new int[numBeaches][2];
        double[] beachRadii  = new double[numBeaches];
        for (int i = 0; i < numBeaches; i++) {
            int edge   = beachRand.nextInt(4);
            int margin = 20;
            int depth  = 40;
            if (edge == 0) {      // Norte
                beachCenters[i][0] = margin + beachRand.nextInt(mapCols - 2 * margin);
                beachCenters[i][1] = margin + beachRand.nextInt(depth);
            } else if (edge == 1) { // Sur
                beachCenters[i][0] = margin + beachRand.nextInt(mapCols - 2 * margin);
                beachCenters[i][1] = mapRows - margin - beachRand.nextInt(depth);
            } else if (edge == 2) { // Oeste
                beachCenters[i][0] = margin + beachRand.nextInt(depth);
                beachCenters[i][1] = margin + beachRand.nextInt(mapRows - 2 * margin);
            } else {                // Este
                beachCenters[i][0] = mapCols - margin - beachRand.nextInt(depth);
                beachCenters[i][1] = margin + beachRand.nextInt(mapRows - 2 * margin);
            }
            beachRadii[i] = 40 + beachRand.nextDouble() * 30;
        }

        generateRivers();

        for (int col = 0; col < mapCols; col++) {
            for (int row = 0; row < mapRows; row++) {

                double elev   = elevNoise.octaveNoise(col * scaleElev, row * scaleElev, 6, 0.55, 2.0);
                double detail = detailNoise.octaveNoise(col * scaleDetail, row * scaleDetail, 3, 0.4, 2.0);
                elev = elev * 0.85 + detail * 0.15;

                // Borde del mapa forzado a océano
                double distX = Math.min(col, mapCols - 1 - col);
                double distY = Math.min(row, mapRows - 1 - row);
                double dist  = Math.min(distX, distY);
                if (dist < 80) {
                    double drop = (80.0 - dist) / 50.0;
                    if (drop > 0) elev -= Math.pow(drop, 1.5) * 1.0;
                }

                // Lagos y ríos
                double lake  = lakeNoise.octaveNoise(col * 0.02, row * 0.02, 2, 0.5, 2.0);
                boolean isLake = (lake > 0.82);
                if (riverWater[col][row] || isLake) {
                    elev = Math.min(elev, SHALLOW_WATER_THRESHOLD - 0.01);
                } else if (riverSand[col][row] || lake > 0.78) {
                    elev = Math.min(elev, SAND_THRESHOLD - 0.01);
                }

                double forest   = forestNoise.octaveNoise(col * scaleForest, row * scaleForest, 4, 0.55, 2.0);
                double dirt     = dirtNoise.octaveNoise(col * scaleDirt,   row * scaleDirt,   2, 0.5,  2.0);
                double clear    = clearNoise.octaveNoise(col * scaleClear,  row * scaleClear,  3, 0.5,  2.0);
                double grassVar = grassVarNoise.octaveNoise(col * scaleGrassVar, row * scaleGrassVar, 2, 0.5, 2.0);

                grassVarCache[col][row] = grassVar;

                // Bioma nieve: círculo con borde orgánico
                boolean snow  = false;
                float sBlend  = 0f;
                if (isSnowyDay) {
                    snow   = true;
                    sBlend = 1.0f;
                } else {
                    double distToSnowCenter = Math.sqrt(Math.pow(col - snowCx, 2) + Math.pow(row - snowCy, 2));
                    // Perturbamos el radio con noise para que el borde se vea orgánico (hasta ±30 tiles)
                    double edgeNoise = tempNoise.octaveNoise(col * 0.02, row * 0.02, 4, 0.5, 2.0);
                    double noisyDist = distToSnowCenter + (edgeNoise - 0.5) * 60.0;
                    if (noisyDist < maxSnowRadius) {
                        snow   = true;
                        sBlend = 1.0f;
                    } else if (noisyDist < maxSnowRadius + 12.0) {
                        double t = 1.0 - (noisyDist - maxSnowRadius) / 12.0;
                        sBlend = (float)Math.pow(Math.max(0, t), 0.30);
                    }
                }
                isSnowBiome[col][row] = snow;
                snowBlend[col][row]   = sBlend;

                // Bioma playa
                boolean beach = false;
                for (int i = 0; i < numBeaches; i++) {
                    double distToBeach = Math.sqrt(Math.pow(col - beachCenters[i][0], 2) + Math.pow(row - beachCenters[i][1], 2));
                    double bEdgeNoise  = tempNoise.octaveNoise((col + 50) * 0.02, (row + 50) * 0.02, 4, 0.5, 2.0);
                    double noisyDist   = distToBeach + (bEdgeNoise - 0.5) * 40.0;
                    if (noisyDist < beachRadii[i]) { beach = true; break; }
                }
                isBeachBiome[col][row] = beach;

                assignTile(col, row, elev, forest, dirt, clear, grassVar, snow, beach);
            }
        }

        // Pases de post-proceso en orden
        limitSandWidth();           // 1. BFS desde agua, convierte arena demasiado lejos a grass
        removeIsolatedTiles();      // 2. elimina tiles de arena/grass aislados
        fixDirtPlacement();         // 3. dirt no puede tocar arena
        computeTransitions();       // 4. detecta bordes grass↔sand y rellena mapTransType/Rot
        clearDirtNearTransitions(); // 5. garantiza 1 tile de grass entre dirt y transiciones
        calculateDeepWaterBlend();  // 6. gradiente de oscuridad para agua profunda
    }

    // =========================================================================
    // Post-proceso 6: gradiente de oscuridad para agua profunda
    // =========================================================================

    private void calculateDeepWaterBlend() {
        int[] distFromNonDeep = new int[mapCols * mapRows];
        java.util.Arrays.fill(distFromNonDeep, Integer.MAX_VALUE);

        ArrayDeque<Integer> queue = new ArrayDeque<>();

        // Sembramos el BFS desde todos los tiles no-deep
        for (int col = 0; col < mapCols; col++) {
            for (int row = 0; row < mapRows; row++) {
                if (!isDeepWater[col][row]) {
                    distFromNonDeep[col * mapRows + row] = 0;
                    queue.add(col * mapRows + row);
                    deepWaterBlend[col][row] = 0f;
                }
            }
        }

        int[] dc = {0, 0, 1, -1};
        int[] dr = {1, -1, 0, 0};

        while (!queue.isEmpty()) {
            int idx = queue.poll();
            int c   = idx / mapRows;
            int r   = idx % mapRows;
            int d   = distFromNonDeep[idx];

            for (int k = 0; k < 4; k++) {
                int nc = c + dc[k], nr = r + dr[k];
                if (nc < 0 || nc >= mapCols || nr < 0 || nr >= mapRows) continue;
                int nIdx = nc * mapRows + nr;
                if (distFromNonDeep[nIdx] != Integer.MAX_VALUE) continue;
                distFromNonDeep[nIdx] = d + 1;
                queue.add(nIdx);
            }
        }

        // Pasamos el blend a cada tile de agua profunda (máximo en 8 tiles de profundidad)
        for (int col = 0; col < mapCols; col++) {
            for (int row = 0; row < mapRows; row++) {
                if (isDeepWater[col][row]) {
                    deepWaterBlend[col][row] = Math.min(1.0f, distFromNonDeep[col * mapRows + row] / 8.0f);
                }
            }
        }
    }

    // =========================================================================
    // Generación de ríos
    // =========================================================================

    private void generateRivers() {
        riverWater = new boolean[mapCols][mapRows];
        riverSand  = new boolean[mapCols][mapRows];

        int numRivers = rand.nextInt(3) + 1;

        if (numRivers == 1) {
            // Un solo río largo que cruza el mapa de borde a borde
            int edge = rand.nextInt(6);
            int x0 = 5, y0 = 5, x1 = 5, y1 = 5;

            int minBoundX = mapCols / 4, maxBoundX = mapCols * 3 / 4, rangeX = maxBoundX - minBoundX;
            int minBoundY = mapRows / 4, maxBoundY = mapRows * 3 / 4, rangeY = maxBoundY - minBoundY;

            if (edge == 0 || edge == 1) {         // Vertical
                x0 = minBoundX + rand.nextInt(rangeX);
                y0 = (edge == 0) ? 5 : mapRows - 5;
                x1 = minBoundX + rand.nextInt(rangeX);
                y1 = (edge == 0) ? mapRows - 5 : 5;
            } else if (edge == 2 || edge == 3) {  // Horizontal
                x0 = (edge == 2) ? 5 : mapCols - 5;
                y0 = minBoundY + rand.nextInt(rangeY);
                x1 = (edge == 2) ? mapCols - 5 : 5;
                y1 = minBoundY + rand.nextInt(rangeY);
            } else if (edge == 4) {               // Diagonal ↘
                x0 = rand.nextInt(minBoundX);
                y0 = 5;
                x1 = maxBoundX + rand.nextInt(minBoundX);
                y1 = mapRows - 5;
            } else {                              // Diagonal ↙
                x0 = maxBoundX + rand.nextInt(minBoundX);
                y0 = 5;
                x1 = rand.nextInt(minBoundX);
                y1 = mapRows - 5;
            }
            drawRiver(x0, y0, x1, y1, true);
        } else {
            // 2 o 3 ríos que nacen en el interior y llegan al borde
            for (int i = 0; i < numRivers; i++) {
                int cx   = 150 + rand.nextInt(mapCols - 300);
                int cy   = 150 + rand.nextInt(mapRows - 300);
                int edge = rand.nextInt(4);
                int x1, y1;
                if      (edge == 0) { x1 = cx + (rand.nextInt(160) - 80); y1 = 5; }
                else if (edge == 1) { x1 = cx + (rand.nextInt(160) - 80); y1 = mapRows - 5; }
                else if (edge == 2) { x1 = 5;            y1 = cy + (rand.nextInt(160) - 80); }
                else                { x1 = mapCols - 5;  y1 = cy + (rand.nextInt(160) - 80); }
                drawRiver(cx, cy, x1, y1, false);
            }
        }
    }

    private void drawRiver(int x0, int y0, int x1, int y1, boolean forceSand) {
        double dist       = Math.hypot(x1 - x0, y1 - y0);
        int steps         = (int)(dist * 2.0);
        double noiseOffX  = rand.nextDouble() * 1000.0;

        PerlinNoise riverWiggle    = new PerlinNoise(seed + rand.nextInt());
        PerlinNoise sandPatchNoise = new PerlinNoise(seed + rand.nextInt());

        double dx     = x1 - x0, dy = y1 - y0;
        double length = Math.hypot(dx, dy);
        double nx     = -dy / length;
        double ny     =  dx / length;

        double waterRadius = 2.2;
        double sandRadius  = forceSand ? 4.5 : 3.5;
        int rMax = (int) Math.ceil(Math.max(waterRadius, sandRadius));

        for (int i = 0; i <= steps; i++) {
            double t  = (double) i / steps;
            double bx = x0 + t * (x1 - x0);
            double by = y0 + t * (y1 - y0);

            // Desplazamiento lateral con noise para que el río serpentee
            double wiggle = riverWiggle.octaveNoise(t * 3.0, noiseOffX, 3, 0.5, 2.0) - 0.5;
            wiggle *= 120.0;

            double cx = bx + nx * wiggle;
            double cy = by + ny * wiggle;

            int icx = (int) cx, icy = (int) cy;

            for (int x = icx - rMax; x <= icx + rMax; x++) {
                for (int y = icy - rMax; y <= icy + rMax; y++) {
                    if (x < 0 || x >= mapCols || y < 0 || y >= mapRows) continue;
                    double d = Math.hypot(x - cx, y - cy);
                    if (d <= waterRadius) {
                        riverWater[x][y] = true;
                    } else if (d <= sandRadius) {
                        if (forceSand || sandPatchNoise.octaveNoise(x * 0.05, y * 0.05, 2, 0.5, 2.0) > 0.55) {
                            riverSand[x][y] = true;
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Asignación de tiles (primer pase)
    // =========================================================================

    private void assignTile(int col, int row, double elev,
                            double forest, double dirt, double clear,
                            double grassVar, boolean snow, boolean beach) {
        mapTreeNum[col][row] = 0;

        if (elev < DEEP_WATER_THRESHOLD) {
            mapTileNum[col][row]     = TILE_WATER0;
            isDeepWater[col][row]    = true;
            isShallowWater[col][row] = false;
            return;
        }
        if (elev < SHALLOW_WATER_THRESHOLD) {
            mapTileNum[col][row]     = TILE_WATER0;
            isDeepWater[col][row]    = false;
            isShallowWater[col][row] = true;
            return;
        }
        isDeepWater[col][row]    = false;
        isShallowWater[col][row] = false;

        double localSandThreshold = beach ? 0.52 : SAND_THRESHOLD;

        if (elev < localSandThreshold) {
            mapTileNum[col][row] = (rand.nextInt(3) < 2) ? TILE_SAND1 : TILE_SAND2;
            if (beach && rand.nextInt(50) == 0)
                mapTreeNum[col][row] = rand.nextBoolean() ? TILE_PALM1 : TILE_PALM2;
            return;
        }

        if (elev >= GRASS_THRESHOLD) {
            mapTileNum[col][row] = TILE_STONE;
            return;
        }

        if (snow) {
            boolean inForest     = forest >= FOREST_THRESHOLD;
            boolean inTransition = !inForest && forest >= FOREST_BLEND_LOW;

            mapTileNum[col][row] = pickGrass(grassVar, true);
            if (inForest) {
                mapTreeNum[col][row] = (clear > 0.68) ? 0 : pickPine();
            } else if (inTransition) {
                mapTreeNum[col][row] = (clear > 0.55) ? 0 : (rand.nextBoolean() ? TILE_PINE1 : TILE_PINE3);
            }
            return;
        }

        boolean inForest     = forest >= FOREST_THRESHOLD;
        boolean inTransition = !inForest && forest >= FOREST_BLEND_LOW;

        if (inForest) {
            mapTileNum[col][row] = pickGrass(grassVar, false);
            mapTreeNum[col][row] = (clear > 0.68) ? 0 : pickTree();
        } else if (inTransition) {
            mapTileNum[col][row] = pickGrass(grassVar, false);
            mapTreeNum[col][row] = (clear > 0.55) ? 0 : (rand.nextBoolean() ? TILE_TREE1 : TILE_TREE3);
        } else {
            // Planicie: posibles parches de tierra
            mapTreeNum[col][row] = 0;
            if (elev > SAND_THRESHOLD + 0.05 && dirt > 0.82) {
                mapTileNum[col][row] = TILE_DIRT1;
            } else {
                mapTileNum[col][row] = pickGrass(grassVar, false);
            }
        }
    }

    // =========================================================================
    // Post-proceso 1: limitar ancho de arena con BFS desde agua
    // =========================================================================

    private void limitSandWidth() {
        int[] distFromWater = new int[mapCols * mapRows];
        java.util.Arrays.fill(distFromWater, Integer.MAX_VALUE);

        ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (int col = 0; col < mapCols; col++) {
            for (int row = 0; row < mapRows; row++) {
                if (isDeepWater[col][row] || isShallowWater[col][row]) {
                    distFromWater[col * mapRows + row] = 0;
                    queue.add(col * mapRows + row);
                }
            }
        }

        int[] dc = {0, 0, 1, -1};
        int[] dr = {1, -1, 0, 0};

        while (!queue.isEmpty()) {
            int idx = queue.poll();
            int c   = idx / mapRows;
            int r   = idx % mapRows;
            int d   = distFromWater[idx];
            for (int k = 0; k < 4; k++) {
                int nc = c + dc[k], nr = r + dr[k];
                if (nc < 0 || nc >= mapCols || nr < 0 || nr >= mapRows) continue;
                int nIdx = nc * mapRows + nr;
                if (distFromWater[nIdx] != Integer.MAX_VALUE) continue;
                distFromWater[nIdx] = d + 1;
                queue.add(nIdx);
            }
        }

        for (int col = 0; col < mapCols; col++) {
            for (int row = 0; row < mapRows; row++) {
                if (!isSand(col, row)) continue;
                int maxWidth = isBeachBiome[col][row] ? 30 : MAX_SAND_WIDTH;
                if (distFromWater[col * mapRows + row] > maxWidth) {
                    mapTileNum[col][row] = pickGrass(grassVarCache[col][row], isSnowBiome[col][row]);
                    mapTreeNum[col][row] = 0;
                }
            }
        }
    }

    // =========================================================================
    // Post-proceso 2: eliminar tiles aislados
    // =========================================================================

    private void removeIsolatedTiles() {
        for (int pass = 0; pass < 2; pass++) {

            // A: tile de arena sin vecinos de arena → pasa a grass
            for (int col = 0; col < mapCols; col++) {
                for (int row = 0; row < mapRows; row++) {
                    if (!isSand(col, row)) continue;
                    int sandNeighbours =
                        (isSand(col, row - 1) ? 1 : 0) +
                        (isSand(col, row + 1) ? 1 : 0) +
                        (isSand(col + 1, row) ? 1 : 0) +
                        (isSand(col - 1, row) ? 1 : 0);
                    if (sandNeighbours == 0)
                        mapTileNum[col][row] = pickGrass(grassVarCache[col][row], isSnowBiome[col][row]);
                }
            }

            // B: tile de grass rodeado por los 4 lados de arena → pasa a arena
            for (int col = 0; col < mapCols; col++) {
                for (int row = 0; row < mapRows; row++) {
                    if (!isGrass(col, row)) continue;
                    if (isSand(col, row - 1) && isSand(col, row + 1) &&
                        isSand(col + 1, row) && isSand(col - 1, row)) {
                        mapTileNum[col][row] = rand.nextBoolean() ? TILE_SAND1 : TILE_SAND2;
                        mapTreeNum[col][row] = 0;
                    }
                }
            }
        }
    }

    // =========================================================================
    // Post-proceso 3: reglas de colocación del dirt
    // =========================================================================

    private void fixDirtPlacement() {
        for (int col = 0; col < mapCols; col++) {
            for (int row = 0; row < mapRows; row++) {
                if (mapTileNum[col][row] != TILE_DIRT1) continue;
                // El dirt no puede estar al lado de arena (en ninguna dirección cardinal)
                if (isSand(col, row - 1) || isSand(col, row + 1) ||
                    isSand(col + 1, row) || isSand(col - 1, row)) {
                    mapTileNum[col][row] = pickGrass(grassVarCache[col][row], isSnowBiome[col][row]);
                }
            }
        }
    }

    // =========================================================================
    // Post-proceso 4: detección de transiciones grass↔sand
    // =========================================================================

    private void computeTransitions() {
        for (int col = 0; col < mapCols; col++) {
            for (int row = 0; row < mapRows; row++) {
                int t = mapTileNum[col][row];
                if (t < TILE_GRASS1 || t > TILE_GRASS4) continue;

                boolean n  = isSand(col,     row - 1);
                boolean s  = isSand(col,     row + 1);
                boolean e  = isSand(col + 1, row);
                boolean w  = isSand(col - 1, row);
                boolean ne = isSand(col + 1, row - 1);
                boolean se = isSand(col + 1, row + 1);
                boolean sw = isSand(col - 1, row + 1);
                boolean nw = isSand(col - 1, row - 1);

                if (!n && !s && !e && !w && !ne && !se && !sw && !nw) continue;

                TransitionRenderer.TransInfo info = TransitionRenderer.classify(n, s, e, w, ne, se, sw, nw);
                mapTransType[col][row] = info.type;
                mapTransRot [col][row] = info.rot;

                // En tiles de transición no puede haber árboles
                if (info.type != TransitionRenderer.TRANS_NONE)
                    mapTreeNum[col][row] = 0;
            }
        }
    }

    // =========================================================================
    // Post-proceso 5: gap obligatorio entre dirt y tiles de transición
    // =========================================================================

    private void clearDirtNearTransitions() {
        for (int col = 0; col < mapCols; col++) {
            for (int row = 0; row < mapRows; row++) {
                if (mapTransType[col][row] == TransitionRenderer.TRANS_NONE) continue;
                // Revisamos los 8 vecinos y eliminamos cualquier dirt adyacente
                for (int dc = -1; dc <= 1; dc++) {
                    for (int dr = -1; dr <= 1; dr++) {
                        if (dc == 0 && dr == 0) continue;
                        int nc = col + dc, nr = row + dr;
                        if (nc < 0 || nc >= mapCols || nr < 0 || nr >= mapRows) continue;
                        if (mapTileNum[nc][nr] == TILE_DIRT1)
                            mapTileNum[nc][nr] = pickGrass(grassVarCache[nc][nr], isSnowBiome[nc][nr]);
                    }
                }
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private boolean isSand(int col, int row) {
        if (col < 0 || col >= mapCols || row < 0 || row >= mapRows) return false;
        int t = mapTileNum[col][row];
        return (t == TILE_SAND1 || t == TILE_SAND2);
    }

    private boolean isGrass(int col, int row) {
        if (col < 0 || col >= mapCols || row < 0 || row >= mapRows) return false;
        int t = mapTileNum[col][row];
        return (t >= TILE_GRASS1 && t <= TILE_GRASS4);
    }

    private int pickGrass(double grassVar, boolean isSnow) {
        // En zonas nevadas favorecemos grass1 (el más blanco) para 85% de los tiles
        if (isSnow && rand.nextDouble() < 0.85) return TILE_GRASS1;
        double v = grassVar + (rand.nextDouble() - 0.5) * 0.15;
        if      (v < 0.30) return TILE_GRASS1;
        else if (v < 0.55) return TILE_GRASS2;
        else if (v < 0.78) return TILE_GRASS3;
        else               return TILE_GRASS4;
    }

    private int pickTree() {
        int r = rand.nextInt(10);
        if      (r < 3) return TILE_TREE1;
        else if (r < 6) return TILE_TREE2;
        else if (r < 8) return TILE_TREE3;
        else            return TILE_TREE4;
    }

    private int pickPine() {
        int r = rand.nextInt(10);
        if      (r < 3) return TILE_PINE1;
        else if (r < 6) return TILE_PINE2;
        else if (r < 8) return TILE_PINE3;
        else if (r < 9) return TILE_PINE4;
        else            return TILE_PINE5;
    }
}
