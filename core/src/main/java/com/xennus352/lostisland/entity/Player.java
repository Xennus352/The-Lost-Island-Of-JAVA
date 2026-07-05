package com.xennus352.lostisland.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Player {
    private Texture texture;
    private float x, y;
    private float speed;

    public Player(float startX, float startY) {
        // 🌟 အရေးကြီး - မင်းရဲ့ assets/ ထဲမှာ သင်္ဘော သို့မဟုတ် player အတွက် 'img.png' ရှိနေရပါမယ်
        texture = new Texture("player.png");
        this.x = startX;
        this.y = startY;
        this.speed = 200f; // တစ်စက္ကန့်ကို ရွေ့မယ့် Pixel အကွာအဝေး
    }


    public void update(float delta) {
        // Keyboard Input တွေကို ဖတ်ပြီး တန်ဖိုးတွေ ပေါင်း/နှုတ် လုပ်ခြင်း (Manual Movement)
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
    }

    public void draw(SpriteBatch batch) {
        // Player ရဲ့ လက်ရှိ တည်နေရာ coordinates အတိုင်း Screen ပေါ်မှာ ဆွဲပေးတာပါ
        batch.draw(texture, x, y);
    }

    public void dispose() {
        texture.dispose();
    }
}
