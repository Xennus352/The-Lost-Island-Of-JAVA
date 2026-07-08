package com.xennus352.lostisland.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.xennus352.lostisland.LostIslandGame;

public class MainMenuScreen implements Screen {
    private final LostIslandGame game;
    private SpriteBatch batch;
    private Texture menuBackground;

    private OrthographicCamera camera;
    private Viewport viewport;

    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    private Rectangle btnStartBounds;
    private Rectangle btnMissionBounds;
    private Rectangle btnSettingsBounds;
    private Vector3 touchPoint;

    public MainMenuScreen(LostIslandGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        menuBackground = new Texture("StartJourney.png");
        touchPoint = new Vector3();

        camera = new OrthographicCamera();
        viewport = new ScalingViewport(Scaling.stretch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();

        btnStartBounds = new Rectangle(400, 280, 200, 45);
        btnMissionBounds = new Rectangle(400, 220, 200, 45);
        btnSettingsBounds = new Rectangle(400, 160, 200, 45);
    }

    @Override
    public void render(float delta) {
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(menuBackground, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch.end();

        // 🌟 FIXED HERE: Changed from GameScreen() to CharacterSelectScreen(game)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new CharacterSelectScreen(game));
        }

        if (Gdx.input.justTouched()) {
            touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPoint);

            if (btnStartBounds.contains(touchPoint.x, touchPoint.y)) {
                game.setScreen(new CharacterSelectScreen(game));
            }
            else if (btnMissionBounds.contains(touchPoint.x, touchPoint.y)) {
                Gdx.app.log("Menu", "Mission Log Clicked!");
            }
            else if (btnSettingsBounds.contains(touchPoint.x, touchPoint.y)) {
                Gdx.app.log("Menu", "Settings Clicked!");
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
        menuBackground.dispose();
    }
}
