package main;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class KeyHandler extends InputAdapter {

    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean enterPressed;

    // Necesitamos el estado previo para detectar el flanco (pressed-once)
    public boolean f11Pressed    = false;
    public boolean f11WasPressed = false;

    public boolean f12Pressed    = false;
    public boolean f12WasPressed = false;

    public UI ui;

    @Override
    public boolean keyTyped(char character) {
        if (ui != null) {
            ui.handleTypedChar(character);
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.W || keycode == Input.Keys.UP)    upPressed    = true;
        if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN)  downPressed  = true;
        if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT)  leftPressed  = true;
        if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) rightPressed = true;

        if (keycode == Input.Keys.ENTER) enterPressed = true;
        if (keycode == Input.Keys.F11)   f11Pressed   = true;
        if (keycode == Input.Keys.F12)   f12Pressed   = true;

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.W || keycode == Input.Keys.UP)    upPressed    = false;
        if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN)  downPressed  = false;
        if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT)  leftPressed  = false;
        if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) rightPressed = false;

        if (keycode == Input.Keys.ENTER) enterPressed = false;
        if (keycode == Input.Keys.F11)   f11Pressed   = false;
        if (keycode == Input.Keys.F12)   f12Pressed   = false;

        return false;
    }
}
