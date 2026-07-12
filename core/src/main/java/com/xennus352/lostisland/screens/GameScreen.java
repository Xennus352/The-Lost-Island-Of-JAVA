package com.xennus352.lostisland.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.xennus352.lostisland.LostIslandGame;
import com.xennus352.lostisland.entity.Player;

public class GameScreen implements Screen {
    private final LostIslandGame game;
    private final String characterPath;
    private final String characterName;

    private SpriteBatch batch;
    private Player player;
    private Texture backgroundTexture;
    private OrthographicCamera camera;
    private Viewport viewport;

    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    public GameScreen(LostIslandGame game, String characterPath, String characterName) {
        this.game = game;
        this.characterPath = characterPath;
        this.characterName = characterName;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        player = new Player(100, 100, characterPath);
        backgroundTexture = new Texture("tileset.png");
        camera = new OrthographicCamera();
        viewport = new ScalingViewport(Scaling.stretch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();

        System.out.println("Selected Character = " + characterName);
    }

    @Override
    public void render(float delta) {
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        ScreenUtils.clear(0, 0, 0, 1);

        player.update(delta);

        batch.begin();
        batch.draw(backgroundTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        player.draw(batch);
        batch.end();
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
        player.dispose();
        backgroundTexture.dispose();
    }
}
