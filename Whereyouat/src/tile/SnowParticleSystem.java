package tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Nieve ambiental suave, similar a la del bioma de nieve de Terraria:
 * pocos copos, lentos, atmosféricos. No es una tormenta.
 */
public class SnowParticleSystem {

    private static final int   MAX_FLAKES     = 45;
    private static final float MIN_SPEED      = 0.3f;
    private static final float MAX_SPEED      = 1.0f;
    private static final float MIN_DRIFT      = -0.3f;
    private static final float MAX_DRIFT      = 0.6f;
    private static final int   MIN_SIZE       = 2;
    private static final int   MAX_SIZE       = 4;
    private static final int   FADE_IN_TICKS  = 90;
    private static final int   FADE_OUT_TICKS = 90;

    private final int screenWidth;
    private final int screenHeight;
    private final Random rand;

    private final List<Snowflake> flakes = new ArrayList<>();

    private float intensity       = 0f;
    private float targetIntensity = 0f;

    private int lastCamX = Integer.MIN_VALUE;
    private int lastCamY = Integer.MIN_VALUE;

    private static Texture circleTex;

    private static Texture getCircleTex() {
        if (circleTex == null) {
            Pixmap p = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
            p.setColor(1f, 1f, 1f, 1f);
            p.fillCircle(4, 4, 3);
            circleTex = new Texture(p);
            circleTex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            p.dispose();
        }
        return circleTex;
    }

    public static void dispose() {
        if (circleTex != null) {
            circleTex.dispose();
            circleTex = null;
        }
    }

    public SnowParticleSystem(int screenWidth, int screenHeight, long seed) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
        this.rand = new Random(seed + 55555);
        for (int i = 0; i < MAX_FLAKES; i++) {
            flakes.add(createFlake(true));
        }
    }

    private Snowflake createFlake(boolean randomY) {
        Snowflake f = new Snowflake();
        f.x           = rand.nextFloat() * screenWidth;
        f.y           = randomY ? rand.nextFloat() * screenHeight : -8f;
        f.speed       = MIN_SPEED + rand.nextFloat() * (MAX_SPEED - MIN_SPEED);
        f.drift       = MIN_DRIFT + rand.nextFloat() * (MAX_DRIFT - MIN_DRIFT);
        f.size        = MIN_SIZE + rand.nextInt(MAX_SIZE - MIN_SIZE + 1);
        f.alpha       = 0.5f + rand.nextFloat() * 0.5f;
        f.wobbleOffset = rand.nextFloat() * (float)(Math.PI * 2);
        f.wobbleSpeed  = 0.015f + rand.nextFloat() * 0.025f;
        f.wobbleAmp    = 0.2f  + rand.nextFloat() * 0.4f;
        f.time         = 0;
        return f;
    }

    public void update(boolean inSnowBiome, float blendFactor, int camX, int camY) {
        if (lastCamX == Integer.MIN_VALUE) {
            lastCamX = camX;
            lastCamY = camY;
        }
        int dCamX = camX - lastCamX;
        int dCamY = camY - lastCamY;
        lastCamX = camX;
        lastCamY = camY;

        // Ignoramos deltas enormes (teleportación al spawn)
        if (Math.abs(dCamX) > 500 || Math.abs(dCamY) > 500) {
            dCamX = 0;
            dCamY = 0;
        }

        targetIntensity = inSnowBiome ? Math.min(1f, blendFactor * 1.5f) : 0f;

        float step = 1f / (inSnowBiome ? FADE_IN_TICKS : FADE_OUT_TICKS);
        if      (intensity < targetIntensity) intensity = Math.min(targetIntensity, intensity + step);
        else if (intensity > targetIntensity) intensity = Math.max(targetIntensity, intensity - step);

        if (intensity < 0.01f) return;

        int activeCount = Math.max(1, (int)(intensity * MAX_FLAKES));

        for (int i = 0; i < flakes.size(); i++) {
            Snowflake f = flakes.get(i);
            if (i >= activeCount) {
                f.y = -20;
                continue;
            }
            f.time++;
            float wobble = f.wobbleAmp * (float)Math.sin(f.wobbleOffset + f.time * f.wobbleSpeed);
            f.x += f.drift + wobble - dCamX;
            f.y += f.speed - dCamY;

            // Wrap en todos los bordes para que los copos recircilen sin cortes bruscos
            while (f.y > screenHeight + 10) { f.y -= (screenHeight + 30); f.x = rand.nextFloat() * screenWidth; }
            while (f.y < -20)              { f.y += (screenHeight + 30); }
            while (f.x < -10)              { f.x += screenWidth + 20; }
            while (f.x > screenWidth + 10) { f.x -= screenWidth + 20; }
        }
    }

    public void draw(SpriteBatch batch) {
        if (intensity < 0.01f) return;

        Texture tex    = getCircleTex();
        int activeCount = Math.max(1, (int)(intensity * MAX_FLAKES));

        for (int i = 0; i < activeCount && i < flakes.size(); i++) {
            Snowflake f = flakes.get(i);
            if (f.y < -8) continue;

            float drawAlpha = f.alpha * intensity;
            float fs = f.size;

            // Sombra suave para dar contraste contra el fondo blanco de nieve
            batch.setColor(0f, 0f, 0f, (80f / 255f) * drawAlpha);
            batch.draw(tex, f.x, f.y + 1, fs, fs);

            batch.setColor(190f / 255f, 230f / 255f, 1f, drawAlpha);
            batch.draw(tex, f.x, f.y, fs, fs);

            if (fs >= 3) {
                batch.setColor(230f / 255f, 250f / 255f, 1f, drawAlpha);
                batch.draw(tex, f.x + fs / 4f, f.y + fs / 4f, Math.max(1, fs / 2f), Math.max(1, fs / 2f));
            }
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private static class Snowflake {
        float x, y, speed, drift, alpha;
        int   size;
        float wobbleOffset, wobbleSpeed, wobbleAmp;
        int   time;
    }
}
