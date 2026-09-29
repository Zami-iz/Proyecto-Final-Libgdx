package main;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import entity.Player;
import tile.SnowParticleSystem;
import tile.TileManager;

public class GamePanel implements ApplicationListener {

    public static final int ORIGINAL_TILE_SIZE = 16;
    public static final int SCALE              = 3;
    public final int tileSize                  = ORIGINAL_TILE_SIZE * SCALE; // 48 px

    // Dimensiones de pantalla, se actualizan en cada resize
    public int screenWidth;
    public int screenHeight;
    public int maxScreenCol;
    public int maxScreenRow;

    public final int maxWorldCol = 512;
    public final int maxWorldRow = 512;

    public long   seed       = 12345L;
    public String seedString = "";
    public String worldName  = "My World";

    public static final int TITLE_STATE        = 0;
    public static final int PLAY_STATE         = 1;
    public static final int WORLD_SELECT_STATE = 4;
    public static final int WORLD_CREATE_STATE = 5;

    public int gameState = TITLE_STATE;

    public KeyHandler         keyH   = new KeyHandler();
    public MouseHandler       mouseH = new MouseHandler();
    public TileManager        tileM;
    public Player             player;
    public SnowParticleSystem snowPS;
    public UI                 ui;

    private SpriteBatch        batch;
    private OrthographicCamera camera;
    private boolean            fullscreen = false;

    @Override
    public void create() {
        batch  = new SpriteBatch();
        camera = new OrthographicCamera();

        updateScreenSize();
        camera.setToOrtho(true, screenWidth, screenHeight);

        Gdx.input.setInputProcessor(new InputMultiplexer(keyH, mouseH));

        ui       = new UI(this);
        keyH.ui  = ui;
    }

    public void updateScreenSize() {
        int w = Gdx.graphics.getWidth()  > 0 ? Gdx.graphics.getWidth()  : 768;
        int h = Gdx.graphics.getHeight() > 0 ? Gdx.graphics.getHeight() : 576;
        screenWidth  = w;
        screenHeight = h;
        maxScreenCol = screenWidth  / tileSize + 1;
        maxScreenRow = screenHeight / tileSize + 1;
    }

    public void toggleFullscreen() {
        if (!fullscreen) {
            Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(mode);
            fullscreen = true;
        } else {
            Gdx.graphics.setWindowedMode(768, 576);
            fullscreen = false;
        }
    }

    public void startGame() {
        if (tileM  != null) tileM.dispose();
        if (player != null) player.dispose();
        SnowParticleSystem.dispose();

        tileM     = new TileManager(this);
        player    = new Player(this, keyH);
        snowPS    = new SnowParticleSystem(screenWidth, screenHeight, seed);
        gameState = PLAY_STATE;
    }

    @Override
    public void render() {
        update();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (gameState == TITLE_STATE
                || gameState == WORLD_SELECT_STATE
                || gameState == WORLD_CREATE_STATE) {
            if (ui != null) ui.draw(batch);

        } else if (tileM != null && player != null) {
            int camX = player.worldX - player.screenX;
            int camY = player.worldY - player.screenY;

            tileM.drawGround(batch, camX, camY);
            tileM.drawTreesBehindPlayer(batch, player.worldY, camX, camY);
            player.draw(batch);
            tileM.drawTreesInFrontOfPlayer(batch, player.worldY, camX, camY);
            if (snowPS != null) snowPS.draw(batch);
            if (ui != null) ui.draw(batch);
        }

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        screenWidth  = width;
        screenHeight = height;
        maxScreenCol = screenWidth  / tileSize + 1;
        maxScreenRow = screenHeight / tileSize + 1;

        camera.setToOrtho(true, screenWidth, screenHeight);
        camera.update();

        if (player != null) {
            player.screenX = screenWidth  / 2 - tileSize / 2;
            player.screenY = screenHeight / 2 - tileSize / 2;
        }
        if (snowPS != null) {
            SnowParticleSystem.dispose();
            snowPS = new SnowParticleSystem(screenWidth, screenHeight, seed);
        }
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        if (batch  != null) { batch.dispose();  batch  = null; }
        if (ui     != null) { ui.dispose();     ui     = null; }
        if (tileM  != null) { tileM.dispose();  tileM  = null; }
        if (player != null) { player.dispose(); player = null; }
        SnowParticleSystem.dispose();
    }

    // -------------------------------------------------------------------------

    public void update() {
        if (keyH.f11Pressed && !keyH.f11WasPressed) toggleFullscreen();
        keyH.f11WasPressed = keyH.f11Pressed;

        if (keyH.f12Pressed && !keyH.f12WasPressed) takeScreenshot();
        keyH.f12WasPressed = keyH.f12Pressed;

        if (gameState == PLAY_STATE && tileM != null && player != null) {
            tileM.update();
            player.update();
            updateSnow();
        } else {
            if (ui != null) ui.update();
        }
    }

    private void updateSnow() {
        if (snowPS == null || tileM == null || player == null) return;
        int footCol = (player.worldX + tileSize / 2) / tileSize;
        int footRow = (player.worldY + tileSize / 2) / tileSize;

        boolean inSnow = false;
        float blend    = 0f;
        if (footCol >= 0 && footCol < maxWorldCol && footRow >= 0 && footRow < maxWorldRow) {
            inSnow = tileM.isSnowBiome[footCol][footRow];
            blend  = tileM.snowBlend[footCol][footRow];
        }

        int camX = player.worldX - player.screenX;
        int camY = player.worldY - player.screenY;
        snowPS.update(inSnow, blend, camX, camY);
    }

    private void takeScreenshot() {
        try {
            com.badlogic.gdx.graphics.Pixmap pixmap = com.badlogic.gdx.graphics.Pixmap.createFromFrameBuffer(0, 0, screenWidth, screenHeight);
            com.badlogic.gdx.files.FileHandle fh    = new com.badlogic.gdx.files.FileHandle("screenshot_" + System.currentTimeMillis() + ".png");
            com.badlogic.gdx.graphics.PixmapIO.writePNG(fh, pixmap);
            pixmap.dispose();
            Gdx.app.log("Screenshot", "Guardado en: " + fh.file().getAbsolutePath());
        } catch (Exception e) {
            Gdx.app.error("Screenshot", "No se pudo guardar el screenshot", e);
        }
    }
}
