package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import main.GamePanel;
import main.KeyHandler;
import tile.MapGenerator;
import tile.TileManager;

public final class Player extends Entity {

    private final GamePanel gp;
    private final KeyHandler keyH;
    private boolean moving = false;

    // El jugador siempre aparece centrado en pantalla
    public int screenX;
    public int screenY;

    private int spawnX;
    private int spawnY;

    private static final int BASE_SPEED    = 4;
    private static final int SHALLOW_SPEED = 2;

    // Hitbox del pie del jugador, relativa a worldX/worldY (esquina superior izquierda del sprite)
    // Caja chica centrada en la base del sprite, usada para la colisión con árboles
    private static final int HIT_OFFSET_X = 14;
    private static final int HIT_OFFSET_Y = 34;
    private static final int HIT_W        = 20;
    private static final int HIT_H        = 12;

    // Margen en tiles para que el jugador no pueda llegar al borde del mapa
    private static final int BOUNDARY_MARGIN = 30;

    private final List<Texture> loadedTextures = new ArrayList<>();

    public Player(GamePanel gp, KeyHandler keyH) {
        this.gp   = gp;
        this.keyH = keyH;

        screenX = gp.screenWidth  / 2 - gp.tileSize / 2;
        screenY = gp.screenHeight / 2 - gp.tileSize / 2;

        setDefaultValues();
        getPlayerImage();
    }

    private void setDefaultValues() {
        speed     = BASE_SPEED;
        direction = "down";

        Random rand = new Random(gp.seed);
        int startCol = 40 + rand.nextInt(gp.maxWorldCol - 80);
        int startRow = 40 + rand.nextInt(gp.maxWorldRow - 80);

        int[] safePos = findSafeSpawn(startCol, startRow);
        spawnX = safePos[0] * gp.tileSize;
        spawnY = safePos[1] * gp.tileSize;
        worldX = spawnX;
        worldY = spawnY;
    }

    public void teleportToSpawn() {
        worldX = spawnX;
        worldY = spawnY;
        speed  = BASE_SPEED;
    }

    // Búsqueda en espiral desde el punto de partida hasta encontrar un tile válido
    private int[] findSafeSpawn(int startCol, int startRow) {
        if (gp.tileM == null) return new int[]{startCol, startRow};

        int maxRadius = Math.max(gp.maxWorldCol, gp.maxWorldRow);

        for (int radius = 0; radius <= maxRadius; radius++) {
            if (radius == 0) {
                if (isSafeSpawnTile(startCol, startRow)) return new int[]{startCol, startRow};
                continue;
            }
            for (int dc = -radius; dc <= radius; dc++) {
                if (isSafeSpawnTile(startCol + dc, startRow - radius))
                    return new int[]{startCol + dc, startRow - radius};
                if (isSafeSpawnTile(startCol + dc, startRow + radius))
                    return new int[]{startCol + dc, startRow + radius};
            }
            for (int dr = -radius + 1; dr <= radius - 1; dr++) {
                if (isSafeSpawnTile(startCol - radius, startRow + dr))
                    return new int[]{startCol - radius, startRow + dr};
                if (isSafeSpawnTile(startCol + radius, startRow + dr))
                    return new int[]{startCol + radius, startRow + dr};
            }
        }

        return new int[]{startCol, startRow};
    }

    private boolean isSafeSpawnTile(int col, int row) {
        int margin = 32;
        if (col < margin || col >= gp.maxWorldCol - margin || row < margin || row >= gp.maxWorldRow - margin)
            return false;
        TileManager tm = gp.tileM;
        if (tm.isDeepWater[col][row] || tm.isShallowWater[col][row]) return false;
        if (tm.mapTreeNum[col][row] > 0) return false;
        if (tm.mapTileNum[col][row] == MapGenerator.TILE_STONE) return false;
        int t = tm.mapTileNum[col][row];
        return t != MapGenerator.TILE_SAND1 && t != MapGenerator.TILE_SAND2;
    }

    private TextureRegion load(String path) {
        Texture tex = new Texture(Gdx.files.internal(path.startsWith("/") ? path.substring(1) : path));
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        loadedTextures.add(tex);
        TextureRegion reg = new TextureRegion(tex);
        reg.flip(false, true);
        return reg;
    }

    private void getPlayerImage() {
        try {
            up1  = load("player/up1.png");  up2  = load("player/up2.png");
            up3  = load("player/up3.png");  up4  = load("player/up4.png");
            up5  = load("player/up5.png");  up6  = load("player/up6.png");
            up7  = load("player/up7.png");  up8  = load("player/up8.png");
            up9  = load("player/up9.png");  up10 = load("player/up10.png");
            up11 = load("player/up11.png"); up12 = load("player/up12.png");
            up13 = load("player/up13.png");

            down1  = load("player/down1.png");  down2  = load("player/down2.png");
            down3  = load("player/down3.png");  down4  = load("player/down4.png");
            down5  = load("player/down5.png");  down6  = load("player/down6.png");
            down7  = load("player/down7.png");  down8  = load("player/down8.png");
            down9  = load("player/down9.png");  down10 = load("player/down10.png");
            down11 = load("player/down11.png"); down12 = load("player/down12.png");
            down13 = load("player/down13.png");

            left1 = load("player/left1.png"); left2 = load("player/left2.png");
            left3 = load("player/left3.png"); left4 = load("player/left4.png");
            left5 = load("player/left5.png"); left6 = load("player/left6.png");
            left7 = load("player/left7.png"); left8 = load("player/left8.png");

            right1 = load("player/right1.png"); right2 = load("player/right2.png");
            right3 = load("player/right3.png"); right4 = load("player/right4.png");
            right5 = load("player/right5.png"); right6 = load("player/right6.png");
            right7 = load("player/right7.png"); right8 = load("player/right8.png");

            idle0 = load("player/idle0.png"); idle1 = load("player/idle1.png");
            idle2 = load("player/idle2.png"); idle3 = load("player/idle3.png");
            idle4 = load("player/idle4.png"); idle5 = load("player/idle5.png");
            idle6 = load("player/idle6.png"); idle7 = load("player/idle7.png");
            idle8 = load("player/idle8.png");

        } catch (RuntimeException e) {
            Gdx.app.error("Player", "Error al cargar las texturas del jugador", e);
        }
    }

    public void update() {
        int footCol    = (worldX + gp.tileSize / 2) / gp.tileSize;
        int footRow    = (worldY + gp.tileSize / 2) / gp.tileSize;
        int waterLevel = gp.tileM.getWaterLevel(footCol, footRow);

        // Agua profunda: teletransportamos al spawn directamente
        if (waterLevel == 2) {
            teleportToSpawn();
            return;
        }

        speed = (waterLevel == 1) ? SHALLOW_SPEED : BASE_SPEED;

        if (keyH.upPressed || keyH.downPressed || keyH.rightPressed || keyH.leftPressed) {
            moving = true;

            int dx = 0, dy = 0;
            if      (keyH.upPressed)    { direction = "up";    dy = -speed; }
            else if (keyH.downPressed)  { direction = "down";  dy =  speed; }
            else if (keyH.leftPressed)  { direction = "left";  dx = -speed; }
            else if (keyH.rightPressed) { direction = "right"; dx =  speed; }

            // Resolución por eje para poder deslizarse contra los árboles
            if (dx != 0) {
                int newX = Math.max(0, Math.min(worldX + dx, gp.maxWorldCol * gp.tileSize - gp.tileSize));
                if (!gp.tileM.collidesWithTree(newX + HIT_OFFSET_X, worldY + HIT_OFFSET_Y, HIT_W, HIT_H))
                    worldX = newX;
            }
            if (dy != 0) {
                int newY = Math.max(0, Math.min(worldY + dy, gp.maxWorldRow * gp.tileSize - gp.tileSize));
                if (!gp.tileM.collidesWithTree(worldX + HIT_OFFSET_X, newY + HIT_OFFSET_Y, HIT_W, HIT_H))
                    worldY = newY;
            }

            // Límite de zona habitable (evitamos que llegue a los bordes vacíos del mapa)
            int minX = BOUNDARY_MARGIN * gp.tileSize;
            int minY = BOUNDARY_MARGIN * gp.tileSize;
            int maxX = (gp.maxWorldCol - BOUNDARY_MARGIN) * gp.tileSize - gp.tileSize;
            int maxY = (gp.maxWorldRow - BOUNDARY_MARGIN) * gp.tileSize - gp.tileSize;

            if (worldX < minX) worldX = minX;
            if (worldY < minY) worldY = minY;
            if (worldX > maxX) worldX = maxX;
            if (worldY > maxY) worldY = maxY;

            spriteCounter++;
            if (direction.equals("up") || direction.equals("down")) {
                if (spriteCounter >= 2.5) {
                    spriteNum++;
                    if (spriteNum > 13) spriteNum = 1;
                    spriteCounter = 0;
                }
            } else {
                if (spriteCounter >= 5) {
                    spriteNum++;
                    if (spriteNum > 8) spriteNum = 1;
                    spriteCounter = 0;
                }
            }

        } else {
            if (moving) {
                spriteNum     = 1;
                spriteCounter = 0;
            }
            moving = false;

            // La animación idle solo existe mirando hacia abajo (dirección inicial)
            if (direction.equals("down")) {
                spriteCounter++;
                if (spriteCounter >= 60) {
                    spriteNum++;
                    if (spriteNum > 9) spriteNum = 1;
                    spriteCounter = 0;
                }
            } else {
                spriteNum     = 1;
                spriteCounter = 0;
            }
        }
    }

    public void draw(SpriteBatch batch) {
        TextureRegion image = null;

        switch (direction) {
            case "up":
                switch (spriteNum) {
                    case 1:  image = up1;  break; case 2:  image = up2;  break;
                    case 3:  image = up3;  break; case 4:  image = up4;  break;
                    case 5:  image = up5;  break; case 6:  image = up6;  break;
                    case 7:  image = up7;  break; case 8:  image = up8;  break;
                    case 9:  image = up9;  break; case 10: image = up10; break;
                    case 11: image = up11; break; case 12: image = up12; break;
                    case 13: image = up13; break;
                    default: image = up1;  break;
                }
                break;

            case "down":
                if (!moving) {
                    switch (spriteNum) {
                        case 1: image = idle0; break; case 2: image = idle1; break;
                        case 3: image = idle2; break; case 4: image = idle3; break;
                        case 5: image = idle4; break; case 6: image = idle5; break;
                        case 7: image = idle6; break; case 8: image = idle7; break;
                        case 9: image = idle8; break;
                        default: image = idle0; break;
                    }
                } else {
                    switch (spriteNum) {
                        case 1:  image = down1;  break; case 2:  image = down2;  break;
                        case 3:  image = down3;  break; case 4:  image = down4;  break;
                        case 5:  image = down5;  break; case 6:  image = down6;  break;
                        case 7:  image = down7;  break; case 8:  image = down8;  break;
                        case 9:  image = down9;  break; case 10: image = down10; break;
                        case 11: image = down11; break; case 12: image = down12; break;
                        case 13: image = down13; break;
                        default: image = down1;  break;
                    }
                }
                break;

            case "right":
                switch (spriteNum) {
                    case 1: image = right1; break; case 2: image = right2; break;
                    case 3: image = right3; break; case 4: image = right4; break;
                    case 5: image = right5; break; case 6: image = right6; break;
                    case 7: image = right7; break; case 8: image = right8; break;
                    default: image = right1; break;
                }
                break;

            case "left":
                switch (spriteNum) {
                    case 1: image = left1; break; case 2: image = left2; break;
                    case 3: image = left3; break; case 4: image = left4; break;
                    case 5: image = left5; break; case 6: image = left6; break;
                    case 7: image = left7; break; case 8: image = left8; break;
                    default: image = left1; break;
                }
                break;

            default:
                image = down1;
                break;
        }

        if (image != null) {
            batch.draw(image, screenX, screenY, gp.tileSize, gp.tileSize);
        }
    }

    public void dispose() {
        for (Texture tex : loadedTextures) {
            if (tex != null) tex.dispose();
        }
        loadedTextures.clear();
    }
}
