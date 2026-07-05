package com.xennus352.lostisland.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.xennus352.lostisland.entity.Player;

public class GameScreen implements Screen {
    private SpriteBatch batch;
    private Player player;

    @Override
    public void show() {
        batch = new SpriteBatch();
        // Player ကို Screen ရဲ့ Coordinates (100, 100) နေရာမှာ စတင် ပေါ်ခိုင်းလိုက်တာပါ
        player = new Player(100, 100);
    }

    @Override
    public void render(float delta) {
        // ၁။ ဂိမ်းရဲ့ နောက်ခံ ပင်လယ်ပြင်အရောင်ကို ဆေးသုတ်မယ် (ဥပမာ - Deep Ocean Blue)
        ScreenUtils.clear(Color.valueOf("#2980b9"));

        // ၂။ Player ရဲ့ လှုပ်ရှားမှု Logic ကို တွက်ချက်မယ်
        player.update(delta);

        // ၃။ Screen ပေါ်မှာ ပြန်ဆွဲမယ်
        batch.begin();
        player.draw(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {}

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
    }
}
