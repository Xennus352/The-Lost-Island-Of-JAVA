package com.xennus352.lostisland.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.xennus352.lostisland.LostIslandGame;
import com.xennus352.lostisland.entity.Player;

public class GameScreen implements Screen {
    private final LostIslandGame game;
    private final String playerGender;

    private SpriteBatch batch;
    private Player player;
    private Texture backgroundTexture;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Virtual Design Resolution (မူလဒီဇိုင်း Size)
    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    // 🌟 ADDED CONSTRUCTOR HERE 🌟
    public GameScreen(LostIslandGame game, String playerGender) {
        this.game = game;
        this.playerGender = playerGender;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // Player ကို ဆောက်တဲ့နေရာမှာ ရွေးချယ်ထားတဲ့ Gender ပါ တစ်ခါတည်း ထည့်ပေးလိုက်လို့ရပါတယ်ဗျာ
        // (Note: Your Player class constructor might need to be updated to accept a String if it doesn't already!)
        player = new Player(100, 100);

        backgroundTexture = new Texture("tileset.png");
        camera = new OrthographicCamera();

        // 🌟 အပြောင်းအလဲ - FitViewport အစား ScalingViewport (Scaling.stretch) ကို ပြောင်းသုံးလိုက်ပါတယ်
        // သူက Window ဘယ်လောက်ချဲ့ချဲ့ အမည်းရောင်ဘောင် လုံးဝမပြဘဲ Screen အပြည့် ဖြန့်ခင်းပေးမှာ ဖြစ်ပါတယ်
        viewport = new ScalingViewport(Scaling.stretch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();
    }

    @Override
    public void render(float delta) {
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // ၁။ ဂိမ်းရဲ့ နောက်ခံ ပင်လယ်ပြင်အရောင်ကို ဆေးသုတ်မယ် (ဥပမာ - Deep Ocean Blue)
        ScreenUtils.clear(0, 0, 0, 1);

        // ၂။ Player ရဲ့ လှုပ်ရှားမှု Logic ကို တွက်ချက်မယ်
        player.update(delta);

        // ၃။ Screen ပေါ်မှာ ပြန်ဆွဲမယ်
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        player.draw(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // Window size ပြောင်းရင် Viewport ကို ဆွဲဆန့်ခိုင်းလိုက်တာပါ
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
