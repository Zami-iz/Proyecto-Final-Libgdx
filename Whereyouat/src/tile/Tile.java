package tile;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Tile {

    public TextureRegion image;
    public boolean collision = false;

    // 0 = tierra firme, 1 = agua poco profunda (ralentiza), 2 = agua profunda (teletransporta al spawn)
    public int waterLevel = 0;
}
