package com.xennus352.lostisland;

import com.badlogic.gdx.Game;
import com.xennus352.lostisland.screens.MainMenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class LostIslandGame extends Game {
    @Override
    public void create() {
        // ဂိမ်းစပွင့်ချင်းမှာ 'this' (LostIslandGame) object ကို ပါရာမီတာ ထည့်ပေးပြီး Main Menu ကို အရင်ပြခိုင်းလိုက်တာပါဗျာ
        setScreen(new MainMenuScreen(this));
    }
}
