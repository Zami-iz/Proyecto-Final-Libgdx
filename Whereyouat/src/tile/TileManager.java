package tile;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import main.GamePanel;

public final class TileManager {

    private final GamePanel gp;

    // Índices de tiles:
    //  0-3  = grass1-4 (suelo)
    //  4    = stone (suelo)
    //  5-8  = water0-3 (suelo, animado)
    //  9    = sand1, 10 = sand2 (suelo)
    //  11   = dirt1 (suelo)
    //  12-15 = tree1-4 (decoración, PNG transparente)
    //  16-20 = pine1-5 (bioma nieve)
    //  21-22 = palm1-2 (bioma playa)
    public Tile[] tile;

    // Cache de grass nevado: [índice grass 0-3][nivel de mezcla 0-10]
    public TextureRegion[][] snowGrass;
    // Cache de agua teñida: [frame 0-3][snowBlend 0-10][deepBlend 0-10]
    public TextureRegion[][][] tintedWater;
    private TextureRegion[] foamCache;

    public int[][] mapTileNum;
    public int[][] mapTreeNum;
    public int[][] mapTransType;
    public int[][] mapTransRot;

    private final TransitionRenderer transRenderer = new TransitionRenderer();

    public boolean[][] isShallowWater;
    public boolean[][] isDeepWater;
    public boolean[][] isSnowBiome;
    public float[][] snowBlend;
    public float[][] deepWaterBlend;

    private int waterAnimCounter = 0;
    private int waterFrame       = 0;
    private static final int WATER_ANIM_SPEED = 18;
    private static final int WATER_FRAMES     = 4;

    // Hitbox del tronco del árbol (relativa al tile, en píxeles lógicos)
    // Caja chica en la base para que el jugador pueda pasar entre troncos
    public static final int TRUNK_HIT_OFFSET_X = 17;
    public static final int TRUNK_HIT_OFFSET_Y = 32;
    public static final int TRUNK_HIT_W        = 14;
    public static final int TRUNK_HIT_H        = 14;

    private final List<Texture> texturesToDispose = new ArrayList<>();

    public TileManager(GamePanel gp) {
        this.gp = gp;
        tile         = new Tile[23];
        snowGrass    = new TextureRegion[4][11];
        mapTileNum   = new int[gp.maxWorldCol][gp.maxWorldRow];
        mapTreeNum   = new int[gp.maxWorldCol][gp.maxWorldRow];
        mapTransType = new int[gp.maxWorldCol][gp.maxWorldRow];
        mapTransRot  = new int[gp.maxWorldCol][gp.maxWorldRow];
        isShallowWater = new boolean[gp.maxWorldCol][gp.maxWorldRow];
        isDeepWater    = new boolean[gp.maxWorldCol][gp.maxWorldRow];
        isSnowBiome    = new boolean[gp.maxWorldCol][gp.maxWorldRow];
        snowBlend      = new float[gp.maxWorldCol][gp.maxWorldRow];
        deepWaterBlend = new float[gp.maxWorldCol][gp.maxWorldRow];
        tintedWater    = new TextureRegion[4][11][11];
        foamCache      = new TextureRegion[12];

        initResources();
        generateMap();
    }

    private void initResources() {
        getTileImage();
        buildFoamImages();

        // Pre-generamos las 11 versiones nevadas de cada tile de grass (0-3)
        for (int i = 0; i <= 3; i++) {
            if (tile[i] == null || tile[i].image == null) continue;
            Pixmap basePix = new Pixmap(Gdx.files.internal("tiles/grass" + (i + 1) + ".png"));
            for (int b = 0; b <= 10; b++) {
                if (b == 0) {
                    snowGrass[i][0] = tile[i].image;
                } else {
                    Pixmap tinted = tintGrassPixmap(basePix, b / 10f);
                    snowGrass[i][b] = makeFlippedRegion(tinted);
                    tinted.dispose();
                }
            }
            basePix.dispose();
        }

        // Pre-generamos versiones teñidas del agua (nieve y profundidad)
        for (int frame = 0; frame < 4; frame++) {
            if (tile[5 + frame] == null || tile[5 + frame].image == null) continue;
            Pixmap basePix = new Pixmap(Gdx.files.internal("tiles/water" + frame + ".png"));
            for (int sb = 0; sb <= 10; sb++) {
                for (int db = 0; db <= 10; db++) {
                    if (sb == 0 && db == 0) {
                        tintedWater[frame][sb][db] = tile[5 + frame].image;
                    } else {
                        Pixmap tinted = tintWaterPixmap(basePix, sb / 10f, db / 10f);
                        tintedWater[frame][sb][db] = makeFlippedRegion(tinted);
                        tinted.dispose();
                    }
                }
            }
            basePix.dispose();
        }
    }

    public TextureRegion makeFlippedRegion(Pixmap pix) {
        Texture tex = new Texture(pix);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        texturesToDispose.add(tex);
        TextureRegion reg = new TextureRegion(tex);
        reg.flip(false, true);
        return reg;
    }

    private void buildFoamImages() {
        Pixmap[] foamPixmaps = new Pixmap[12];
        for (int i = 0; i < 12; i++) {
            foamPixmaps[i] = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        }
        java.util.Random r = new java.util.Random(12345);
        int c1 = 0xFFFFFF00 | 160;
        int c2 = 0xFFFFFF00 | 80;

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                boolean noise1 = r.nextDouble() > 0.3;
                boolean noise2 = r.nextDouble() > 0.6;

                // Lados cardinales (N,E,S,W)
                if (j == 0  && noise1) foamPixmaps[0].drawPixel(i, j, c1);
                if (j == 1  && noise2) foamPixmaps[0].drawPixel(i, j, c2);
                if (i == 15 && noise1) foamPixmaps[1].drawPixel(i, j, c1);
                if (i == 14 && noise2) foamPixmaps[1].drawPixel(i, j, c2);
                if (j == 15 && noise1) foamPixmaps[2].drawPixel(i, j, c1);
                if (j == 14 && noise2) foamPixmaps[2].drawPixel(i, j, c2);
                if (i == 0  && noise1) foamPixmaps[3].drawPixel(i, j, c1);
                if (i == 1  && noise2) foamPixmaps[3].drawPixel(i, j, c2);

                // Esquinas externas (NE,SE,SW,NW)
                if ((j == 0  || i == 15) && noise1) foamPixmaps[4].drawPixel(i, j, c1);
                if ((j == 1  || i == 14) && noise2) foamPixmaps[4].drawPixel(i, j, c2);
                if ((j == 15 || i == 15) && noise1) foamPixmaps[5].drawPixel(i, j, c1);
                if ((j == 14 || i == 14) && noise2) foamPixmaps[5].drawPixel(i, j, c2);
                if ((j == 15 || i == 0)  && noise1) foamPixmaps[6].drawPixel(i, j, c1);
                if ((j == 14 || i == 1)  && noise2) foamPixmaps[6].drawPixel(i, j, c2);
                if ((j == 0  || i == 0)  && noise1) foamPixmaps[7].drawPixel(i, j, c1);
                if ((j == 1  || i == 1)  && noise2) foamPixmaps[7].drawPixel(i, j, c2);

                // Esquinas internas (diagonales)
                if (i == 15 && j == 0  && noise1) foamPixmaps[8].drawPixel(i, j, c1);
                if (i == 15 && j == 15 && noise1) foamPixmaps[9].drawPixel(i, j, c1);
                if (i == 0  && j == 15 && noise1) foamPixmaps[10].drawPixel(i, j, c1);
                if (i == 0  && j == 0  && noise1) foamPixmaps[11].drawPixel(i, j, c1);
            }
        }
        for (int i = 0; i < 12; i++) {
            foamCache[i] = makeFlippedRegion(foamPixmaps[i]);
            foamPixmaps[i].dispose();
        }
    }

    public static Pixmap tintWaterPixmap(Pixmap src, float snowBlend, float deepBlend) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        Pixmap tinted = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        int snR = 100, snG = 200, snB = 255;
        int dpR = 10,  dpG = 40,  dpB = 140;

        float[] hsb = new float[3];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = src.getPixel(x, y);
                int a    = rgba & 0xFF;
                if (a == 0) { tinted.drawPixel(x, y, rgba); continue; }

                int r = (rgba >>> 24) & 0xFF;
                int g = (rgba >>> 16) & 0xFF;
                int b = (rgba >>>  8) & 0xFF;

                rgbToHsb(r, g, b, hsb);

                // Solo teñimos píxeles azulados (hue 0.45-0.75 = zona agua)
                if (hsb[0] >= 0.45f && hsb[0] <= 0.75f) {
                    float totalBlend = snowBlend + deepBlend;
                    if (totalBlend > 0) {
                        float snWeight = snowBlend / totalBlend;
                        float dpWeight = deepBlend / totalBlend;

                        int targetR = (int)(snR * snWeight + dpR * dpWeight);
                        int targetG = (int)(snG * snWeight + dpG * dpWeight);
                        int targetB = (int)(snB * snWeight + dpB * dpWeight);

                        float blendForce = Math.min(1.0f, totalBlend);

                        int newR = Math.min(255, Math.max(0, (int)(r + (targetR - r) * blendForce)));
                        int newG = Math.min(255, Math.max(0, (int)(g + (targetG - g) * blendForce)));
                        int newB = Math.min(255, Math.max(0, (int)(b + (targetB - b) * blendForce)));

                        tinted.drawPixel(x, y, (newR << 24) | (newG << 16) | (newB << 8) | a);
                    } else {
                        tinted.drawPixel(x, y, rgba);
                    }
                } else {
                    tinted.drawPixel(x, y, rgba);
                }
            }
        }
        return tinted;
    }

    public static Pixmap tintGrassPixmap(Pixmap src, float blend) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        Pixmap tinted = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        int targetR = 230, targetG = 240, targetB = 255;
        float[] hsb = new float[3];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = src.getPixel(x, y);
                int a    = rgba & 0xFF;
                if (a == 0) { tinted.drawPixel(x, y, rgba); continue; }

                int r = (rgba >>> 24) & 0xFF;
                int g = (rgba >>> 16) & 0xFF;
                int b = (rgba >>>  8) & 0xFF;

                rgbToHsb(r, g, b, hsb);

                // Solo teñimos los verdes del grass (hue 0.16-0.45, saturación mínima 0.10)
                if (hsb[0] >= 0.16f && hsb[0] <= 0.45f && hsb[1] >= 0.10f) {
                    int newR = Math.min(255, Math.max(0, (int)(r + (targetR - r) * blend)));
                    int newG = Math.min(255, Math.max(0, (int)(g + (targetG - g) * blend)));
                    int newB = Math.min(255, Math.max(0, (int)(b + (targetB - b) * blend)));

                    tinted.drawPixel(x, y, (newR << 24) | (newG << 16) | (newB << 8) | a);
                } else {
                    tinted.drawPixel(x, y, rgba);
                }
            }
        }
        return tinted;
    }

    private static void rgbToHsb(int r, int g, int b, float[] hsbvals) {
        float hue, saturation, brightness;
        int cmax = Math.max(r, Math.max(g, b));
        int cmin = Math.min(r, Math.min(g, b));

        brightness = cmax / 255.0f;
        saturation = (cmax != 0) ? (float)(cmax - cmin) / cmax : 0;

        if (saturation == 0) {
            hue = 0;
        } else {
            float redc   = (float)(cmax - r) / (cmax - cmin);
            float greenc = (float)(cmax - g) / (cmax - cmin);
            float bluec  = (float)(cmax - b) / (cmax - cmin);
            if      (r == cmax) hue = bluec - greenc;
            else if (g == cmax) hue = 2.0f + redc - bluec;
            else                hue = 4.0f + greenc - redc;
            hue /= 6.0f;
            if (hue < 0) hue += 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
    }

    private void getTileImage() {
        try {
            tile[0]  = mk("tiles/grass1.png", 0);
            tile[1]  = mk("tiles/grass2.png", 0);
            tile[2]  = mk("tiles/grass3.png", 0);
            tile[3]  = mk("tiles/grass4.png", 0);
            tile[4]  = mk("tiles/stone.png",  0);
            tile[5]  = mk("tiles/water0.png", 2);
            tile[6]  = mk("tiles/water1.png", 2);
            tile[7]  = mk("tiles/water2.png", 2);
            tile[8]  = mk("tiles/water3.png", 2);
            tile[9]  = mk("tiles/sand.png",   0);
            tile[10] = mk("tiles/sand2.png",  0);
            tile[11] = mk("tiles/dirt1.png",  0);
            tile[12] = mk("tiles/tree1.png",  0);
            tile[13] = mk("tiles/tree2.png",  0);
            tile[14] = mk("tiles/tree3.png",  0);
            tile[15] = mk("tiles/tree4.png",  0);
            tile[16] = mk("tiles/pine1.png",  0);
            tile[17] = mk("tiles/pine2.png",  0);
            tile[18] = mk("tiles/pine3.png",  0);
            tile[19] = mk("tiles/pine4.png",  0);
            tile[20] = mk("tiles/pine5.png",  0);
            tile[21] = mk("tiles/palm1.png",  0);
            tile[22] = mk("tiles/palm2.png",  0);

            transRenderer.loadImages();
        } catch (RuntimeException e) {
            Gdx.app.error("TileManager", "Error al cargar las imágenes de tiles", e);
        }
    }

    private Tile mk(String path, int waterLevel) {
        Tile t    = new Tile();
        Texture tex = new Texture(Gdx.files.internal(path.startsWith("/") ? path.substring(1) : path));
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        texturesToDispose.add(tex);
        t.image      = new TextureRegion(tex);
        t.image.flip(false, true);
        t.waterLevel = waterLevel;
        t.collision  = false;
        return t;
    }

    private void generateMap() {
        MapGenerator gen = new MapGenerator(gp.maxWorldCol, gp.maxWorldRow, gp.seed, gp.seedString);
        gen.generate();
        mapTileNum     = gen.mapTileNum;
        mapTreeNum     = gen.mapTreeNum;
        mapTransType   = gen.mapTransType;
        mapTransRot    = gen.mapTransRot;
        isShallowWater = gen.isShallowWater;
        isDeepWater    = gen.isDeepWater;
        isSnowBiome    = gen.isSnowBiome;
        snowBlend      = gen.snowBlend;
        deepWaterBlend = gen.deepWaterBlend;
    }

    // 0 = tierra firme, 1 = agua poco profunda, 2 = agua profunda
    public int getWaterLevel(int col, int row) {
        if (col < 0 || col >= gp.maxWorldCol || row < 0 || row >= gp.maxWorldRow) return 0;
        if (isDeepWater[col][row])    return 2;
        if (isShallowWater[col][row]) return 1;
        return 0;
    }

    public boolean hasTree(int col, int row) {
        if (col < 0 || col >= gp.maxWorldCol || row < 0 || row >= gp.maxWorldRow) return false;
        return mapTreeNum[col][row] > 0;
    }

    public void update() {
        waterAnimCounter++;
        if (waterAnimCounter >= WATER_ANIM_SPEED) {
            waterAnimCounter = 0;
            waterFrame = (waterFrame + 1) % WATER_FRAMES;
        }
    }

    public void drawGround(SpriteBatch batch, int camX, int camY) {
        int startCol = Math.max(0, camX / gp.tileSize);
        int startRow = Math.max(0, camY / gp.tileSize);
        int endCol   = Math.min(gp.maxWorldCol - 1, startCol + gp.maxScreenCol + 2);
        int endRow   = Math.min(gp.maxWorldRow - 1, startRow + gp.maxScreenRow + 2);

        for (int wc = startCol; wc <= endCol; wc++) {
            for (int wr = startRow; wr <= endRow; wr++) {
                int sx = wc * gp.tileSize - camX;
                int sy = wr * gp.tileSize - camY;

                int idx     = mapTileNum[wc][wr];
                boolean isWater = (idx >= 5 && idx <= 8);
                if (isWater) idx = 5 + waterFrame;

                float blend  = snowBlend[wc][wr];
                boolean hasSnow = (blend > 0.01f && !isDeepWater[wc][wr] && !isShallowWater[wc][wr]);

                if (isWater) {
                    int sb = Math.min(10, (int)(snowBlend[wc][wr] * 10));
                    int db = Math.min(10, (int)(deepWaterBlend[wc][wr] * 10));
                    if (tintedWater[waterFrame][sb][db] != null) {
                        batch.draw(tintedWater[waterFrame][sb][db], sx, sy, gp.tileSize, gp.tileSize);
                    } else if (tile[idx] != null && tile[idx].image != null) {
                        batch.draw(tile[idx].image, sx, sy, gp.tileSize, gp.tileSize);
                    }
                } else if (hasSnow && idx >= 0 && idx <= 3) {
                    int step = Math.min(10, (int)(blend * 10));
                    if (snowGrass[idx][step] != null) {
                        batch.draw(snowGrass[idx][step], sx, sy, gp.tileSize, gp.tileSize);
                    } else if (tile[idx] != null && tile[idx].image != null) {
                        batch.draw(tile[idx].image, sx, sy, gp.tileSize, gp.tileSize);
                    }
                } else {
                    if (tile[idx] != null && tile[idx].image != null)
                        batch.draw(tile[idx].image, sx, sy, gp.tileSize, gp.tileSize);
                }

                // Overlay de transición grass↔sand
                int transType = mapTransType[wc][wr];
                if (transType != TransitionRenderer.TRANS_NONE) {
                    int step = hasSnow ? Math.min(10, (int)(blend * 10)) : 0;
                    TextureRegion transImg = transRenderer.getImage(transType, mapTransRot[wc][wr], step);
                    if (transImg != null)
                        batch.draw(transImg, sx, sy, gp.tileSize, gp.tileSize);
                }

                // Espuma en los bordes del agua
                if (isWater) {
                    boolean n  = (wr > 0)                    && (getWaterLevel(wc,     wr - 1) == 0);
                    boolean s  = (wr < gp.maxWorldRow - 1)   && (getWaterLevel(wc,     wr + 1) == 0);
                    boolean w  = (wc > 0)                    && (getWaterLevel(wc - 1, wr)     == 0);
                    boolean e  = (wc < gp.maxWorldCol - 1)   && (getWaterLevel(wc + 1, wr)     == 0);
                    boolean nw = (wc > 0 && wr > 0)                              && (getWaterLevel(wc - 1, wr - 1) == 0);
                    boolean ne = (wc < gp.maxWorldCol - 1 && wr > 0)             && (getWaterLevel(wc + 1, wr - 1) == 0);
                    boolean sw = (wc > 0 && wr < gp.maxWorldRow - 1)             && (getWaterLevel(wc - 1, wr + 1) == 0);
                    boolean se = (wc < gp.maxWorldCol - 1 && wr < gp.maxWorldRow - 1) && (getWaterLevel(wc + 1, wr + 1) == 0);

                    if (n && !e && !w) batch.draw(foamCache[0],  sx, sy, gp.tileSize, gp.tileSize);
                    if (e && !n && !s) batch.draw(foamCache[1],  sx, sy, gp.tileSize, gp.tileSize);
                    if (s && !e && !w) batch.draw(foamCache[2],  sx, sy, gp.tileSize, gp.tileSize);
                    if (w && !n && !s) batch.draw(foamCache[3],  sx, sy, gp.tileSize, gp.tileSize);

                    if (n && e) batch.draw(foamCache[4],  sx, sy, gp.tileSize, gp.tileSize);
                    if (s && e) batch.draw(foamCache[5],  sx, sy, gp.tileSize, gp.tileSize);
                    if (s && w) batch.draw(foamCache[6],  sx, sy, gp.tileSize, gp.tileSize);
                    if (n && w) batch.draw(foamCache[7],  sx, sy, gp.tileSize, gp.tileSize);

                    if (ne && !n && !e) batch.draw(foamCache[8],  sx, sy, gp.tileSize, gp.tileSize);
                    if (se && !s && !e) batch.draw(foamCache[9],  sx, sy, gp.tileSize, gp.tileSize);
                    if (sw && !s && !w) batch.draw(foamCache[10], sx, sy, gp.tileSize, gp.tileSize);
                    if (nw && !n && !w) batch.draw(foamCache[11], sx, sy, gp.tileSize, gp.tileSize);
                }
            }
        }
    }

    public void drawTreesBehindPlayer(SpriteBatch batch, int playerWorldY, int camX, int camY) {
        drawTreesFiltered(batch, playerWorldY, camX, camY, false);
    }

    public void drawTreesInFrontOfPlayer(SpriteBatch batch, int playerWorldY, int camX, int camY) {
        drawTreesFiltered(batch, playerWorldY, camX, camY, true);
    }

    private void drawTreesFiltered(SpriteBatch batch, int playerWorldY, int camX, int camY, boolean inFront) {
        int startCol = Math.max(0, camX / gp.tileSize);
        int startRow = Math.max(0, camY / gp.tileSize);
        int endCol   = Math.min(gp.maxWorldCol - 1, startCol + gp.maxScreenCol + 2);
        int endRow   = Math.min(gp.maxWorldRow - 1, startRow + gp.maxScreenRow + 2);

        int playerFootY = playerWorldY + gp.tileSize;

        for (int wc = startCol; wc <= endCol; wc++) {
            for (int wr = startRow; wr <= endRow; wr++) {
                int treeIdx = mapTreeNum[wc][wr];
                if (treeIdx == 0) continue;

                // El árbol va "delante" del jugador si su pie está más abajo que el del jugador
                boolean treeInFront = (wr * gp.tileSize + gp.tileSize > playerFootY);
                if (treeInFront != inFront) continue;

                int sx = wc * gp.tileSize - camX;
                int sy = wr * gp.tileSize - camY;

                if (tile[treeIdx] != null && tile[treeIdx].image != null)
                    batch.draw(tile[treeIdx].image, sx, sy, gp.tileSize, gp.tileSize);
            }
        }
    }

    public boolean collidesWithTree(int wx, int wy, int ww, int wh) {
        int leftTile   = Math.max(0, wx / gp.tileSize);
        int rightTile  = Math.min(gp.maxWorldCol - 1, (wx + ww) / gp.tileSize);
        int topTile    = Math.max(0, wy / gp.tileSize);
        int bottomTile = Math.min(gp.maxWorldRow - 1, (wy + wh) / gp.tileSize);

        for (int tc = leftTile; tc <= rightTile; tc++) {
            for (int tr = topTile; tr <= bottomTile; tr++) {
                if (mapTreeNum[tc][tr] == 0) continue;

                int trunkX = tc * gp.tileSize + TRUNK_HIT_OFFSET_X;
                int trunkY = tr * gp.tileSize + TRUNK_HIT_OFFSET_Y;
                int trunkR = trunkX + TRUNK_HIT_W;
                int trunkB = trunkY + TRUNK_HIT_H;

                if (wx < trunkR && wx + ww > trunkX && wy < trunkB && wy + wh > trunkY)
                    return true;
            }
        }
        return false;
    }

    public void dispose() {
        for (Texture tex : texturesToDispose) {
            if (tex != null) tex.dispose();
        }
        texturesToDispose.clear();
        transRenderer.dispose();
    }
}
