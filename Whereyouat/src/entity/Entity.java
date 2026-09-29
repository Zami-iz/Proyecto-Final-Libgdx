package entity;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Entity {

    public int worldX, worldY;
    public int speed;

    public TextureRegion down1, down2, down3, down4, down5, down6, down7, down8,
            down9, down10, down11, down12, down13,
            up1, up2, up3, up4, up5, up6, up7, up8,
            up9, up10, up11, up12, up13,
            left1, left2, left3, left4, left5, left6, left7, left8,
            right1, right2, right3, right4, right5, right6, right7, right8,
            idle0, idle1, idle2, idle3, idle4, idle5, idle6, idle7, idle8;

    public String direction;

    public double spriteCounter = 0;
    public int    spriteNum     = 1;
}
