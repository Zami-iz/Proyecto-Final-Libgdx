package main;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public final class UI {

    private final GamePanel gp;

    private BitmapFont pixelFont;
    private BitmapFont pixelFontBig;
    private BitmapFont pixelFontSmall;
    private final GlyphLayout layout = new GlyphLayout();

    private Texture whiteTex;
    private TextureRegion whitePixel;
    private Texture skyGradTex;

    private int titleCommandNum = 0;

    private List<WorldSaveManager.WorldInfo> worldList;
    private int     worldSelectIndex  = 0;
    private int     worldScrollOffset = 0;
    private boolean draggingScrollbar = false;
    private static final int WORLDS_VISIBLE = 4;

    private String  createNameInput  = "";
    private String  createSeedInput  = "";
    private int     createCommandNum = 0;
    private int     textFocus        = 0;
    private static final int CREATE_MAX_NAME = 20;
    private static final int CREATE_MAX_SEED = 18;

    private static Color col(int r, int g, int b, int a) { return new Color(r / 255f, g / 255f, b / 255f, a / 255f); }
    private static Color col(int r, int g, int b)        { return new Color(r / 255f, g / 255f, b / 255f, 1f); }

    private static final Color COL_TITLE    = col(255, 230, 80);
    private static final Color COL_SELECTED = col(255, 190, 50);
    private static final Color COL_NORMAL   = col(200, 220, 200);
    private static final Color COL_SHADOW   = col(20, 10, 5, 200);

    public UI(GamePanel gp) {
        this.gp = gp;
        loadFonts();
        initPrimitives();
    }

    private void initPrimitives() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(1f, 1f, 1f, 1f);
        p.fill();
        whiteTex   = new Texture(p);
        whitePixel = new TextureRegion(whiteTex);
        p.dispose();

        // Gradiente de cielo: 1x2 con interpolación lineal
        Pixmap gpPix = new Pixmap(1, 2, Pixmap.Format.RGBA8888);
        gpPix.setColor(8f / 255f, 10f / 255f, 30f / 255f, 1f);
        gpPix.drawPixel(0, 0);
        gpPix.setColor(15f / 255f, 35f / 255f, 15f / 255f, 1f);
        gpPix.drawPixel(0, 1);
        skyGradTex = new Texture(gpPix);
        skyGradTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        gpPix.dispose();
    }

    private void loadFonts() {
        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/PressStart2P-Regular.ttf"));
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.flip      = true;
            param.minFilter = TextureFilter.Nearest;
            param.magFilter = TextureFilter.Nearest;

            param.size    = 12; pixelFont      = generator.generateFont(param);
            param.size    = 20; pixelFontBig   = generator.generateFont(param);
            param.size    = 7;  pixelFontSmall = generator.generateFont(param);

            generator.dispose();
        } catch (RuntimeException e) {
            Gdx.app.error("UI", "Error al cargar la fuente TTF, usando fallback", e);
            pixelFont      = new BitmapFont(true);
            pixelFontBig   = new BitmapFont(true);
            pixelFontSmall = new BitmapFont(true);
        }
    }

    public void update() {
        switch (gp.gameState) {
            case GamePanel.TITLE_STATE:        handleTitle();       break;
            case GamePanel.WORLD_SELECT_STATE: handleWorldSelect(); break;
            case GamePanel.WORLD_CREATE_STATE: handleWorldCreate(); break;
            default: break;
        }
    }

    private void handleTitle() {
        int w = gp.screenWidth;
        int h = gp.screenHeight;
        int menuY = h / 2 + 10;

        int mx = gp.mouseH.mouseX;
        int my = gp.mouseH.mouseY;
        boolean clicked = gp.mouseH.isLeftClicked();

        int boxW = 240, boxH = 36;
        int boxX = w / 2 - boxW / 2;

        int playY = menuY - boxH / 2 - 2;
        boolean hoverPlay = (mx >= boxX && mx <= boxX + boxW && my >= playY && my <= playY + boxH);

        int quitY = menuY + 52 - boxH / 2 - 2;
        boolean hoverQuit = (mx >= boxX && mx <= boxX + boxW && my >= quitY && my <= quitY + boxH);

        titleCommandNum = -1;
        if (hoverPlay) titleCommandNum = 0;
        if (hoverQuit) titleCommandNum = 1;

        if (clicked) {
            if (hoverPlay) {
                refreshWorldList();
                gp.gameState = GamePanel.WORLD_SELECT_STATE;
            } else if (hoverQuit) {
                Gdx.app.exit();
            }
        }
    }

    private void refreshWorldList() {
        worldList         = WorldSaveManager.listWorlds();
        worldSelectIndex  = 0;
        worldScrollOffset = 0;
    }

    private void handleWorldSelect() {
        int total = (worldList == null ? 0 : worldList.size()) + 1;
        int w = gp.screenWidth;
        int h = gp.screenHeight;
        int panelX = w / 8;
        int panelW = w * 3 / 4;
        int itemH  = 52;
        int startY = 85;

        if (gp.mouseH.scrollAmount != 0) {
            worldScrollOffset += gp.mouseH.scrollAmount;
            if (worldScrollOffset < 0) worldScrollOffset = 0;
            if (worldScrollOffset > total - WORLDS_VISIBLE)
                worldScrollOffset = Math.max(0, total - WORLDS_VISIBLE);
            gp.mouseH.resetScroll();
        }

        int mx = gp.mouseH.mouseX;
        int my = gp.mouseH.mouseY;
        boolean clicked = gp.mouseH.isLeftClicked();

        if (total > WORLDS_VISIBLE) {
            int scrollBarX = panelX + panelW + 10;
            int scrollBarY = startY;
            int scrollBarH = (WORLDS_VISIBLE + 1) * itemH - 4;
            int thumbH     = Math.max(30, (int)(((float)(WORLDS_VISIBLE + 1) / total) * scrollBarH));
            int maxOffset  = Math.max(1, total - WORLDS_VISIBLE);

            if (gp.mouseH.leftPressed) {
                if (!draggingScrollbar) {
                    if (mx >= scrollBarX - 10 && mx <= scrollBarX + 30 && my >= scrollBarY && my <= scrollBarY + scrollBarH)
                        draggingScrollbar = true;
                }
                if (draggingScrollbar) {
                    float relativeY = (my - scrollBarY - thumbH / 2f);
                    float trackH    = scrollBarH - thumbH;
                    if (trackH > 0) {
                        float fraction = Math.max(0, Math.min(1, relativeY / trackH));
                        worldScrollOffset = Math.round(fraction * maxOffset);
                    }
                }
            } else {
                if (draggingScrollbar) {
                    draggingScrollbar = false;
                    clicked = false;
                }
            }
        } else {
            draggingScrollbar = false;
        }

        int backBtnW = 140, backBtnH = 36;
        int backBtnX = panelX;
        int backBtnY = h - 60;
        boolean hoverBack = (mx >= backBtnX && mx <= backBtnX + backBtnW && my >= backBtnY && my <= backBtnY + backBtnH);

        if (clicked && hoverBack) {
            gp.gameState = GamePanel.TITLE_STATE;
            return;
        }

        worldSelectIndex = -1;
        for (int i = 0; i < WORLDS_VISIBLE + 1; i++) {
            int listIndex = worldScrollOffset + i;
            if (listIndex >= total) break;

            int iy = startY + i * itemH;

            if (mx >= panelX && mx <= panelX + panelW && my >= iy && my <= iy + itemH - 4) {
                worldSelectIndex = listIndex;

                if (clicked) {
                    boolean isDeleteBox = false;
                    if (listIndex < (worldList == null ? 0 : worldList.size())) {
                        int delBoxX = panelX + panelW - 40;
                        int delBoxY = iy + 8;
                        int delBoxW = 24;
                        int delBoxH = 24;
                        isDeleteBox = (mx >= delBoxX && mx <= delBoxX + delBoxW && my >= delBoxY && my <= delBoxY + delBoxH);
                    }

                    if (isDeleteBox) {
                        WorldSaveManager.deleteWorld(worldList.get(listIndex).fileName);
                        refreshWorldList();
                        break;
                    } else if (listIndex == (worldList == null ? 0 : worldList.size())) {
                        openWorldCreate();
                    } else {
                        WorldSaveManager.WorldInfo info = worldList.get(listIndex);
                        gp.seed      = info.seed;
                        gp.worldName = info.name;
                        gp.startGame();
                    }
                }
            }
        }
    }

    private void openWorldCreate() {
        createNameInput  = "";
        createSeedInput  = "";
        createCommandNum = -1;
        textFocus        = 0;
        gp.gameState     = GamePanel.WORLD_CREATE_STATE;
    }

    private void handleWorldCreate() {
        int w  = gp.screenWidth;
        int mx = gp.mouseH.mouseX;
        int my = gp.mouseH.mouseY;
        boolean clicked = gp.mouseH.isLeftClicked();

        int fieldW   = w * 2 / 3;
        int fieldX   = w / 2 - fieldW / 2;
        int boxW     = 240, boxBtnH = 36;
        int btnX     = w / 2 - boxW / 2;
        int boxH     = 28;

        int y        = 100;
        int nameBoxY = y + 16;
        boolean hoverName = (mx >= fieldX && mx <= fieldX + fieldW && my >= nameBoxY && my <= nameBoxY + boxH);
        y = nameBoxY + boxH + 22;

        int seedBoxY = y + 16;
        boolean hoverSeed = (mx >= fieldX && mx <= fieldX + fieldW && my >= seedBoxY && my <= seedBoxY + boxH);
        y = seedBoxY + boxH + 28;

        int randBtnY = y + 8 - boxBtnH / 2 - 2;
        boolean hoverRand = (mx >= btnX && mx <= btnX + boxW && my >= randBtnY && my <= randBtnY + boxBtnH);
        y += 48;

        int createBtnY = y + 8 - boxBtnH / 2 - 2;
        boolean hoverCreate = (mx >= btnX && mx <= btnX + boxW && my >= createBtnY && my <= createBtnY + boxBtnH);
        y += 48;

        int backBtnY = y + 8 - boxBtnH / 2 - 2;
        boolean hoverBack = (mx >= btnX && mx <= btnX + boxW && my >= backBtnY && my <= backBtnY + boxBtnH);

        createCommandNum = -1;
        if      (hoverName)   createCommandNum = 0;
        else if (hoverSeed)   createCommandNum = 1;
        else if (hoverRand)   createCommandNum = 2;
        else if (hoverCreate) createCommandNum = 3;
        else if (hoverBack)   createCommandNum = 4;

        if (clicked) {
            if      (hoverName) { textFocus = 0; }
            else if (hoverSeed) { textFocus = 1; }
            else if (hoverRand) {
                createSeedInput = String.valueOf(Math.abs(new java.util.Random().nextLong() % 999999999L));
                textFocus = 1;
            }
            else if (hoverCreate) { doCreateWorld(); }
            else if (hoverBack) {
                refreshWorldList();
                gp.gameState = GamePanel.WORLD_SELECT_STATE;
            }
        }
    }

    private void doCreateWorld() {
        String name = createNameInput.trim().isEmpty() ? "My World" : createNameInput.trim();
        long seed;
        try {
            seed = Long.parseLong(createSeedInput.trim());
        } catch (NumberFormatException e) {
            String trimmed = createSeedInput.trim();
            seed = trimmed.isEmpty() ? System.currentTimeMillis() : trimmed.hashCode();
        }
        WorldSaveManager.saveWorld(name, seed);
        gp.seed      = seed;
        gp.worldName = name;
        gp.startGame();
    }

    public void draw(SpriteBatch batch) {
        switch (gp.gameState) {
            case GamePanel.TITLE_STATE:        drawTitle(batch);       break;
            case GamePanel.WORLD_SELECT_STATE: drawWorldSelect(batch); break;
            case GamePanel.WORLD_CREATE_STATE: drawWorldCreate(batch); break;
            case GamePanel.PLAY_STATE:         drawHUD();              break;
            default: break;
        }
    }

    private void drawTitle(SpriteBatch batch) {
        int w = gp.screenWidth, h = gp.screenHeight;

        batch.setColor(Color.WHITE);
        batch.draw(skyGradTex, 0, 0, w, h);

        java.util.Random sr = new java.util.Random(42);
        for (int i = 0; i < 120; i++) {
            fillRect(batch, sr.nextInt(w), sr.nextInt(h * 2 / 3), sr.nextInt(2) + 1, sr.nextInt(2) + 1, col(255, 255, 255, 140));
        }

        int titleY = h / 4;
        drawCentered(batch, pixelFontBig, "WHERE YOU AT?", w, titleY + 3, COL_SHADOW);
        drawCentered(batch, pixelFontBig, "WHERE YOU AT?", w, titleY, COL_TITLE);
        drawCentered(batch, pixelFont, "Somewhere Out There", w, titleY + 44, col(160, 220, 130));

        fillRect(batch, w / 4f, titleY + 58, w / 2f, 1, col(80, 130, 80, 120));

        int menuY = h / 2 + 10;
        drawMenuBtn(batch, "PLAY", w, menuY,      titleCommandNum == 0);
        drawMenuBtn(batch, "QUIT", w, menuY + 52, titleCommandNum == 1);

        drawCentered(batch, pixelFontSmall, "Use MOUSE to navigate and click", w, h - 28, col(100, 100, 100));
        drawCentered(batch, pixelFontSmall, "F11 - Fullscreen", w, h - 12, col(70, 70, 70));
    }

    private void drawWorldSelect(SpriteBatch batch) {
        int w = gp.screenWidth, h = gp.screenHeight;

        fillRect(batch, 0, 0, w, h, col(10, 15, 10));

        drawCentered(batch, pixelFontBig, "SELECT WORLD", w, 55 + 3, COL_SHADOW);
        drawCentered(batch, pixelFontBig, "SELECT WORLD", w, 55, COL_TITLE);
        fillRect(batch, w / 6f, 65, w * 2f / 3f, 1, col(60, 100, 60, 150));

        int panelX    = w / 8;
        int panelW    = w * 3 / 4;
        int itemH     = 52;
        int startY    = 85;
        int worldCount = (worldList == null ? 0 : worldList.size());
        int total     = worldCount + 1;

        for (int i = 0; i < WORLDS_VISIBLE + 1; i++) {
            int listIndex = worldScrollOffset + i;
            if (listIndex >= total) break;

            int iy       = startY + i * itemH;
            boolean selected = (listIndex == worldSelectIndex);

            Color boxColor = selected ? col(40, 70, 40, 200) : col(18, 28, 18, 180);
            fillRect(batch, panelX, iy, panelW, itemH - 4, boxColor);

            if (selected) {
                drawRect(batch, panelX, iy, panelW, itemH - 4, 1, col(100, 180, 80, 180));
                pixelFont.setColor(COL_SELECTED);
                pixelFont.draw(batch, ">", panelX - 18, iy + 28 - pixelFont.getCapHeight());
            }

            if (listIndex == worldCount) {
                Color c = selected ? COL_SELECTED : col(140, 200, 120);
                drawCentered(batch, pixelFont, "+ NEW WORLD", w, iy + 30, c);
            } else {
                WorldSaveManager.WorldInfo info = worldList.get(listIndex);
                pixelFont.setColor(selected ? COL_SELECTED : COL_NORMAL);
                pixelFont.draw(batch, info.name, panelX + 14, iy + 22 - pixelFont.getCapHeight());

                pixelFontSmall.setColor(col(140, 160, 120));
                pixelFontSmall.draw(batch, "Seed: " + info.seed, panelX + 14, iy + 38 - pixelFontSmall.getCapHeight());
                pixelFontSmall.draw(batch, WorldSaveManager.formatDate(info.created),
                        panelX + panelW - 130, iy + 38 - pixelFontSmall.getCapHeight());

                int delBoxX = panelX + panelW - 40;
                int delBoxY = iy + 8;
                int delBoxW = 24;
                int delBoxH = 24;
                int mx = gp.mouseH.mouseX, my = gp.mouseH.mouseY;
                boolean isDelHover = (mx >= delBoxX && mx <= delBoxX + delBoxW && my >= delBoxY && my <= delBoxY + delBoxH);

                fillRect(batch, delBoxX, delBoxY, delBoxW, delBoxH, isDelHover ? col(220, 70, 70, 220) : col(180, 50, 50, 150));
                pixelFontSmall.setColor(Color.WHITE);
                pixelFontSmall.draw(batch, "X", delBoxX + 9, delBoxY + 16 - pixelFontSmall.getCapHeight());
            }
        }

        int backBtnW = 140, backBtnH = 36;
        int backBtnX = panelX;
        int backBtnY = h - 60;
        int mx = gp.mouseH.mouseX, my = gp.mouseH.mouseY;
        boolean hoverBack = (mx >= backBtnX && mx <= backBtnX + backBtnW && my >= backBtnY && my <= backBtnY + backBtnH);

        fillRect(batch, backBtnX, backBtnY, backBtnW, backBtnH, hoverBack ? col(255, 200, 50, 50) : col(30, 40, 30, 180));
        drawRect(batch, backBtnX, backBtnY, backBtnW, backBtnH, 1, hoverBack ? COL_SELECTED : COL_NORMAL);
        pixelFont.setColor(hoverBack ? COL_SELECTED : COL_NORMAL);
        pixelFont.draw(batch, "< BACK", backBtnX + 25, backBtnY + 24 - pixelFont.getCapHeight());

        if (total > WORLDS_VISIBLE) {
            int scrollBarX = panelX + panelW + 10;
            int scrollBarY = startY;
            int scrollBarH = (WORLDS_VISIBLE + 1) * itemH - 4;
            int thumbH     = Math.max(30, (int)(((float)(WORLDS_VISIBLE + 1) / total) * scrollBarH));
            int maxOffset  = Math.max(1, total - WORLDS_VISIBLE);
            int thumbY     = scrollBarY + (int)(((float)worldScrollOffset / maxOffset) * (scrollBarH - thumbH));

            fillRect(batch, scrollBarX, scrollBarY, 12, scrollBarH, col(30, 40, 30, 180));
            fillRect(batch, scrollBarX, thumbY, 12, thumbH, col(100, 150, 100, 200));
        }

        drawCentered(batch, pixelFontSmall, "CLICK to select    WHEEL to scroll", w, h - 16, col(90, 90, 90));
    }

    private void drawWorldCreate(SpriteBatch batch) {
        int w = gp.screenWidth, h = gp.screenHeight;

        fillRect(batch, 0, 0, w, h, col(10, 15, 10));

        drawCentered(batch, pixelFontBig, "NEW WORLD", w, 55 + 3, COL_SHADOW);
        drawCentered(batch, pixelFontBig, "NEW WORLD", w, 55, COL_TITLE);
        fillRect(batch, w / 6f, 65, w * 2f / 3f, 1, col(60, 100, 60, 150));

        int fieldW = w * 2 / 3;
        int fieldX = w / 2 - fieldW / 2;
        int y      = 100;

        y = drawCreateField(batch, "WORLD NAME", createNameInput, fieldX, y, fieldW, textFocus == 0, "Default: My World");
        y += 22;

        y = drawCreateField(batch, "SEED", createSeedInput, fieldX, y, fieldW, textFocus == 1, "Numbers or text  (empty = random)");
        y += 28;

        drawMenuBtn(batch, "[ RANDOM SEED ]", w, y + 8, createCommandNum == 2);
        y += 48;
        drawMenuBtn(batch, "CREATE WORLD", w, y + 8, createCommandNum == 3);
        y += 48;
        drawMenuBtn(batch, "< BACK", w, y + 8, createCommandNum == 4);

        drawCentered(batch, pixelFontSmall, "Use the keyboard to type  |  Use MOUSE to select", w, h - 16, col(90, 90, 90));
    }

    private int drawCreateField(SpriteBatch batch, String label, String value,
                                int x, int y, int w, boolean selected, String hint) {
        pixelFontSmall.setColor(selected ? COL_SELECTED : col(160, 180, 140));
        pixelFontSmall.draw(batch, label, x, y + 10 - pixelFontSmall.getCapHeight());
        y += 16;

        int boxH = 28;
        fillRect(batch, x, y, w, boxH, selected ? col(30, 55, 30, 230) : col(18, 28, 18, 180));
        drawRect(batch, x, y, w, boxH, 1, selected ? col(120, 200, 80, 200) : col(50, 80, 40, 150));

        String display = value.isEmpty() ? hint : value;
        pixelFontSmall.setColor(value.isEmpty() ? col(80, 100, 70) : COL_NORMAL);
        pixelFontSmall.draw(batch, display, x + 8, y + 19 - pixelFontSmall.getCapHeight());

        // Cursor parpadeante cada 500ms
        if (selected && (System.currentTimeMillis() / 500) % 2 == 0) {
            layout.setText(pixelFontSmall, value);
            fillRect(batch, x + 8 + layout.width, y + 8, 2, 14, COL_SELECTED);
        }

        return y + boxH;
    }

    private void drawHUD() {
        // Sin HUD por ahora
    }

    private void drawMenuBtn(SpriteBatch batch, String text, int screenW, int y, boolean selected) {
        int boxW = 240, boxH = 36;
        int boxX = screenW / 2 - boxW / 2;
        int boxY = y - boxH / 2 - 2;

        if (selected) {
            fillRect(batch, boxX - 4, boxY - 4, boxW + 8, boxH + 8, col(255, 200, 50, 50));
            drawRect(batch, boxX, boxY, boxW, boxH, 1, col(255, 200, 50, 140));
            pixelFont.setColor(COL_SELECTED);
            pixelFont.draw(batch, ">", boxX - 20, y + 5 - pixelFont.getCapHeight());
            pixelFont.draw(batch, "<", boxX + boxW + 6, y + 5 - pixelFont.getCapHeight());
        }

        drawCentered(batch, pixelFont, text, screenW, y + 5, selected ? COL_SELECTED : COL_NORMAL);
    }

    private void drawCentered(SpriteBatch batch, BitmapFont font, String text, int screenW, float y, Color c) {
        layout.setText(font, text);
        font.setColor(c);
        font.draw(batch, text, (screenW - layout.width) / 2f, y - font.getCapHeight());
    }

    private void fillRect(SpriteBatch batch, float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(whitePixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    private void drawRect(SpriteBatch batch, float x, float y, float w, float h, float t, Color c) {
        batch.setColor(c);
        batch.draw(whitePixel, x,         y,         w, t);
        batch.draw(whitePixel, x,         y + h - t, w, t);
        batch.draw(whitePixel, x,         y,         t, h);
        batch.draw(whitePixel, x + w - t, y,         t, h);
        batch.setColor(Color.WHITE);
    }

    public void handleTypedChar(char c) {
        if (gp.gameState != GamePanel.WORLD_CREATE_STATE) return;
        if (textFocus == 0) {
            if (c == '\b') {
                if (!createNameInput.isEmpty())
                    createNameInput = createNameInput.substring(0, createNameInput.length() - 1);
            } else if (createNameInput.length() < CREATE_MAX_NAME && c >= 32 && c < 127) {
                createNameInput += c;
            }
        } else if (textFocus == 1) {
            if (c == '\b') {
                if (!createSeedInput.isEmpty())
                    createSeedInput = createSeedInput.substring(0, createSeedInput.length() - 1);
            } else if (createSeedInput.length() < CREATE_MAX_SEED && c >= 32 && c < 127) {
                createSeedInput += c;
            }
        }
    }

    public void dispose() {
        if (whiteTex     != null) { whiteTex.dispose();     whiteTex     = null; }
        if (skyGradTex   != null) { skyGradTex.dispose();   skyGradTex   = null; }
        if (pixelFont    != null) { pixelFont.dispose();    pixelFont    = null; }
        if (pixelFontBig != null) { pixelFontBig.dispose(); pixelFontBig = null; }
        if (pixelFontSmall != null) { pixelFontSmall.dispose(); pixelFontSmall = null; }
    }
}
