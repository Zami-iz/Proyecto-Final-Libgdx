package tile;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Maneja las transiciones de grass↔sand: carga los sprites base, los pre-rota
 * y genera 11 niveles de mezcla con nieve para cada variante.
 */
public final class TransitionRenderer {

    // Tipos de transición (guardados en mapTransType)
    public static final int TRANS_NONE       = 0;
    public static final int TRANS_SIDE       = 1;
    public static final int TRANS_OUT_CORNER = 2;
    public static final int TRANS_IN_CORNER  = 3;

    // Rotaciones (0,1,2,3 = 0°,90°,180°,270° CW)
    public static final int ROT_0   = 0;
    public static final int ROT_90  = 1;
    public static final int ROT_180 = 2;
    public static final int ROT_270 = 3;

    // Cache pre-rotado: [tipo 0-2][rotación 0-3][nivel de mezcla 0-10]
    // tipo 0 = SIDE, tipo 1 = OUT_CORNER, tipo 2 = IN_CORNER
    private final TextureRegion[][][] rotated = new TextureRegion[3][4][11];

    private final List<Texture> textures = new ArrayList<>();

    public TransitionRenderer() {}

    public void loadImages() {
        Pixmap side = new Pixmap(Gdx.files.internal("tiles/transi_grasstosand_sides.png"));
        Pixmap outC = new Pixmap(Gdx.files.internal("tiles/transi_grasstosand_out_corner.png"));
        Pixmap inC  = new Pixmap(Gdx.files.internal("tiles/transi_grasstosand_in_corner.png"));

        for (int r = 0; r < 4; r++) {
            Pixmap s0 = rotateCW(side, r);
            Pixmap o0 = rotateCW(outC, r);
            Pixmap i0 = rotateCW(inC,  r);

            for (int b = 0; b <= 10; b++) {
                float blend = b / 10f;
                Pixmap sPix = (b == 0) ? s0 : TileManager.tintGrassPixmap(s0, blend);
                Pixmap oPix = (b == 0) ? o0 : TileManager.tintGrassPixmap(o0, blend);
                Pixmap iPix = (b == 0) ? i0 : TileManager.tintGrassPixmap(i0, blend);

                rotated[0][r][b] = makeFlippedRegion(sPix);
                rotated[1][r][b] = makeFlippedRegion(oPix);
                rotated[2][r][b] = makeFlippedRegion(iPix);

                if (b > 0) {
                    sPix.dispose();
                    oPix.dispose();
                    iPix.dispose();
                }
            }

            s0.dispose();
            o0.dispose();
            i0.dispose();
        }

        side.dispose();
        outC.dispose();
        inC.dispose();
    }

    private TextureRegion makeFlippedRegion(Pixmap pix) {
        Texture tex = new Texture(pix);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        textures.add(tex);
        TextureRegion reg = new TextureRegion(tex);
        reg.flip(false, true);
        return reg;
    }

    private Pixmap rotateCW(Pixmap src, int rot) {
        int w = src.getWidth();
        int h = src.getHeight();
        Pixmap dst = new Pixmap(w, h, src.getFormat());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = src.getPixel(x, y);
                int nx = x, ny = y;
                if      (rot == 1) { nx = h - 1 - y; ny = x; }
                else if (rot == 2) { nx = w - 1 - x; ny = h - 1 - y; }
                else if (rot == 3) { nx = y;          ny = w - 1 - x; }
                dst.drawPixel(nx, ny, pixel);
            }
        }
        return dst;
    }

    public TextureRegion getImage(int transType, int rot, int step) {
        int typeIdx = transType - 1;
        if (typeIdx < 0 || typeIdx >= 3) return null;
        if (rot < 0 || rot >= 4)         return null;
        if (step < 0 || step >= 11)      return null;
        return rotated[typeIdx][rot][step];
    }

    public void dispose() {
        for (Texture tex : textures) {
            if (tex != null) tex.dispose();
        }
        textures.clear();
    }

    public static final class TransInfo {
        public final int type;
        public final int rot;
        public TransInfo(int type, int rot) { this.type = type; this.rot = rot; }
    }

    /**
     * Analiza los 8 vecinos de un tile GRASS y devuelve el tipo de transición
     * que corresponde según qué vecinos son arena.
     */
    public static TransInfo classify(boolean n, boolean s, boolean e, boolean w,
                                     boolean ne, boolean se, boolean sw, boolean nw) {
        int cardinalCount = (n ? 1 : 0) + (s ? 1 : 0) + (e ? 1 : 0) + (w ? 1 : 0);

        if (cardinalCount == 1) {
            if (n) return new TransInfo(TRANS_SIDE, ROT_0);
            if (e) return new TransInfo(TRANS_SIDE, ROT_90);
            if (s) return new TransInfo(TRANS_SIDE, ROT_180);
            return new TransInfo(TRANS_SIDE, ROT_270);
        }

        if (cardinalCount == 2) {
            if (n && e) return new TransInfo(TRANS_OUT_CORNER, ROT_0);
            if (e && s) return new TransInfo(TRANS_OUT_CORNER, ROT_90);
            if (s && w) return new TransInfo(TRANS_OUT_CORNER, ROT_180);
            if (n && w) return new TransInfo(TRANS_OUT_CORNER, ROT_270);
            if (n && s) return new TransInfo(TRANS_SIDE, ROT_0);
            if (e && w) return new TransInfo(TRANS_SIDE, ROT_90);
        }

        if (cardinalCount >= 3) return new TransInfo(TRANS_NONE, ROT_0);

        if (ne) return new TransInfo(TRANS_IN_CORNER, ROT_0);
        if (se) return new TransInfo(TRANS_IN_CORNER, ROT_90);
        if (sw) return new TransInfo(TRANS_IN_CORNER, ROT_180);
        if (nw) return new TransInfo(TRANS_IN_CORNER, ROT_270);

        return new TransInfo(TRANS_NONE, ROT_0);
    }
}
