package com.xennus352.lostisland.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Player {
    private Texture texture;
    private float x, y;
    private float speed;
    // Virtual Design Resolution (မူလဒီဇိုင်း Size)
    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    public Player(float startX, float startY) {
        // 🌟 အရေးကြီး - မင်းရဲ့ assets/ ထဲမှာ သင်္ဘော သို့မဟုတ် player အတွက် 'img.png' ရှိနေရပါမယ်
        texture = new Texture("player.png");
        this.x = startX;
        this.y = startY;
        this.speed = 200f; // တစ်စက္ကန့်ကို ရွေ့မယ့် Pixel အကွာအဝေး
    }


    public void update(float delta) {
        // 1. Handle Movement Input
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            y += speed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            y -= speed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            x -= speed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            x += speed * delta;
        }

        // 2. Define Boundaries
        // The player's X/Y is the bottom-left corner of the image.
        // So, max X is the screen width minus the player's width.
        float minX = 0;
        float maxX = VIRTUAL_WIDTH - texture.getWidth();
        float minY = 0;
        float maxY = VIRTUAL_HEIGHT - texture.getHeight();

        // 3. Apply Clamping every frame
        this.x = MathUtils.clamp(this.x, minX, maxX);
        this.y = MathUtils.clamp(this.y, minY, maxY);
    }

    public void draw(SpriteBatch batch) {
        // Player ရဲ့ လက်ရှိ တည်နေရာ coordinates အတိုင်း Screen ပေါ်မှာ ဆွဲပေးတာပါ
        batch.draw(texture, x, y);
    }

    public void dispose() {
        texture.dispose();
    }
}
