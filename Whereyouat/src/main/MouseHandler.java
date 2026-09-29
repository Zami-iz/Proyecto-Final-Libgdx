package main;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class MouseHandler extends InputAdapter {

    public int mouseX, mouseY;
    public boolean leftPressed;
    public boolean rightPressed;
    private boolean leftClicked; // verdadero por un solo frame
    public int scrollAmount;     // positivo = scroll abajo, negativo = arriba

    // Se consume al leerlo para que no se procese dos veces en el mismo frame
    public boolean isLeftClicked() {
        if (leftClicked) {
            leftClicked = false;
            return true;
        }
        return false;
    }

    public void resetScroll() {
        scrollAmount = 0;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        mouseX = screenX;
        mouseY = screenY;
        if (button == Input.Buttons.LEFT)  leftPressed  = true;
        if (button == Input.Buttons.RIGHT) rightPressed = true;
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        mouseX = screenX;
        mouseY = screenY;
        if (button == Input.Buttons.LEFT) {
            leftPressed = false;
            leftClicked = true;
        }
        if (button == Input.Buttons.RIGHT) {
            rightPressed = false;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        mouseX = screenX;
        mouseY = screenY;
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mouseX = screenX;
        mouseY = screenY;
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        scrollAmount = (int) amountY;
        return false;
    }
}
