package com.xennus352.lostisland.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.xennus352.lostisland.LostIslandGame;

public class CharacterSelectScreen implements Screen {
    private final LostIslandGame game;
    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont titleFont;
    private GlyphLayout glyphLayout;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Vector3 touchPoint;

    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    private Texture bgTexture;
    private Texture continueButtonTexture;
    private Texture buttonHoverTexture;

    // Character system
    private Texture[] characterTextures;
    private String[] characterNames;
    private String[] characterFolders;
    private int selectedIndex = 0;
    private int totalCharacters;

    // Carousel state
    private float[] characterScales;
    private float[] targetScales;
    private float[] currentXOffsets;
    private float[] targetXOffsets;
    private float characterWidth = 120f;
    private float characterHeight = 120f;
    private float centerX;
    private float baseY;

    private static final float ANIMATION_SPEED = 0.15f;
    private boolean isAnimating = false;

    private Rectangle btnContinueBounds;
    private boolean isHoveringContinue = false;
    private boolean isSelected = false;

    public CharacterSelectScreen(LostIslandGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.8f);
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);
        glyphLayout = new GlyphLayout();

        touchPoint = new Vector3();

        camera = new OrthographicCamera();
        viewport = new ScalingViewport(Scaling.stretch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();

        centerX = VIRTUAL_WIDTH / 2f;
        baseY = 150f;

        bgTexture = new Texture("Background/CharacterSelectionBg.png");
        continueButtonTexture = new Texture("ui/button.png");
        buttonHoverTexture = new Texture("ui/button.png");

        // Character data
        characterNames = new String[] {
            "Warrior",
            "Mage",
            "Rogue",
            "Archer",
            "Paladin"
        };

        characterFolders = new String[] {
            "Fighter",
            "Pirate/OldCap",
            "Pirate/Jack",
            "Archer",
            "Knight"
        };

        characterTextures = new Texture[] {
            new Texture("Fighter/south.png"),
            new Texture("Pirate/OldCap/south.png"),
            new Texture("Pirate/Jack/south.png"),
            new Texture("player.png"),
            new Texture("p4.png")
        };

        totalCharacters = characterTextures.length;

        // Initialize animation arrays
        characterScales = new float[3];
        targetScales = new float[3];
        currentXOffsets = new float[3];
        targetXOffsets = new float[3];

        updateCarouselPositions(selectedIndex);

        btnContinueBounds = new Rectangle(centerX - 100, 50, 200, 55);
        isSelected = false;
    }

    private void updateCarouselPositions(int centerIndex) {
        float spacing = 180f;
        float smallScale = 1.3f;
        float largeScale = 1.6f;

        int leftIndex = (centerIndex - 1 + totalCharacters) % totalCharacters;
        int centerIdx = centerIndex;
        int rightIndex = (centerIndex + 1) % totalCharacters;

        targetXOffsets[0] = centerX - spacing;
        targetXOffsets[1] = centerX;
        targetXOffsets[2] = centerX + spacing;

        targetScales[0] = smallScale;
        targetScales[1] = largeScale;
        targetScales[2] = smallScale;

        if (!isAnimating) {
            for (int i = 0; i < 3; i++) {
                currentXOffsets[i] = targetXOffsets[i];
                characterScales[i] = targetScales[i];
            }
        }
    }

    private void animateCarousel(int newIndex) {
        isAnimating = true;
        float spacing = 180f;
        float smallScale = 1.3f;
        float largeScale = 1.6f;

        targetXOffsets[0] = centerX - spacing;
        targetXOffsets[1] = centerX;
        targetXOffsets[2] = centerX + spacing;

        targetScales[0] = smallScale;
        targetScales[1] = largeScale;
        targetScales[2] = smallScale;
    }

    private void updateAnimation() {
        if (isAnimating) {
            boolean allDone = true;
            for (int i = 0; i < 3; i++) {
                currentXOffsets[i] = MathUtils.lerp(currentXOffsets[i], targetXOffsets[i], ANIMATION_SPEED);
                characterScales[i] = MathUtils.lerp(characterScales[i], targetScales[i], ANIMATION_SPEED);
                if (Math.abs(currentXOffsets[i] - targetXOffsets[i]) > 0.5f ||
                    Math.abs(characterScales[i] - targetScales[i]) > 0.01f) {
                    allDone = false;
                }
            }
            if (allDone) {
                for (int i = 0; i < 3; i++) {
                    currentXOffsets[i] = targetXOffsets[i];
                    characterScales[i] = targetScales[i];
                }
                isAnimating = false;
            }
        }
    }

    private int[] getCurrentCharacterIndices() {
        int leftIndex = (selectedIndex - 1 + totalCharacters) % totalCharacters;
        int centerIdx = selectedIndex;
        int rightIndex = (selectedIndex + 1) % totalCharacters;
        return new int[]{leftIndex, centerIdx, rightIndex};
    }

    @Override
    public void render(float delta) {
        updateAnimation();

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        batch.begin();

        batch.draw(bgTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Title
        titleFont.setColor(Color.WHITE);
        String title = "SELECT YOUR CHARACTER";
        glyphLayout.setText(titleFont, title);
        titleFont.draw(batch, title, (VIRTUAL_WIDTH - glyphLayout.width) / 2f, VIRTUAL_HEIGHT - 30);

        drawCharacterCarousel();
        drawNavigationArrows();
        drawContinueButton();
        drawCharacterInfo();

        batch.end();
        handleInput();
    }

    private void drawCharacterCarousel() {
        int[] indices = getCurrentCharacterIndices();

        for (int i = 0; i < 3; i++) {
            int charIndex = indices[i];
            float scale = characterScales[i];
            float x = currentXOffsets[i] - (characterWidth * scale) / 2f;
            float y = baseY - (characterHeight * scale) / 2f;

            if (i == 1) {
                batch.setColor(1, 0.8f, 0.3f, 0.2f + 0.1f * (float)Math.sin(Gdx.graphics.getDeltaTime() * 3f));
                batch.draw(characterTextures[charIndex], x - 20, y - 20,
                    characterWidth * scale + 40, characterHeight * scale + 40);
                batch.setColor(Color.WHITE);
            }

            batch.draw(characterTextures[charIndex], x, y,
                characterWidth * scale, characterHeight * scale);
        }
    }

    private void drawNavigationArrows() {
        float arrowY = baseY + 20;
        font.setColor(isHoveringLeftArrow() ? Color.GOLD : Color.WHITE);
        font.draw(batch, "<", 60, arrowY);

        font.setColor(isHoveringRightArrow() ? Color.GOLD : Color.WHITE);
        glyphLayout.setText(font, ">");
        font.draw(batch, ">", VIRTUAL_WIDTH - 60 - glyphLayout.width, arrowY);
        font.setColor(Color.WHITE);
    }

    private void drawCharacterInfo() {
        String name = characterNames[selectedIndex];
        glyphLayout.setText(font, name);
        float nameX = centerX - glyphLayout.width / 2f;
        float nameY = baseY - 40;

        batch.setColor(0, 0, 0, 0.5f);
        batch.draw(continueButtonTexture, nameX - 20, nameY - 8,
            glyphLayout.width + 40, glyphLayout.height + 16);
        batch.setColor(Color.WHITE);

        font.setColor(Color.GOLD);
        font.draw(batch, name, nameX, nameY + glyphLayout.height);
        font.setColor(Color.WHITE);
    }

    private void drawContinueButton() {
        String buttonText = isSelected ? "PLAY" : "SELECT";
        float scale = isHoveringContinue ? 1.05f : 1f;

        float x = btnContinueBounds.x + (btnContinueBounds.width - btnContinueBounds.width * scale) / 2f;
        float y = btnContinueBounds.y + (btnContinueBounds.height - btnContinueBounds.height * scale) / 2f;

        batch.setColor(isHoveringContinue ? new Color(1, 0.9f, 0.5f, 1) :
            isSelected ? new Color(0.2f, 0.8f, 0.2f, 1) : Color.WHITE);
        batch.draw(continueButtonTexture, x, y,
            btnContinueBounds.width * scale, btnContinueBounds.height * scale);
        batch.setColor(Color.WHITE);

        glyphLayout.setText(font, buttonText);
        float textX = btnContinueBounds.x + (btnContinueBounds.width - glyphLayout.width) / 2f;
        float textY = btnContinueBounds.y + (btnContinueBounds.height + glyphLayout.height) / 2f;

        font.setColor(Color.BLACK);
        font.draw(batch, buttonText, textX + 2, textY - 2);
        font.setColor(isSelected ? Color.GREEN : Color.WHITE);
        font.draw(batch, buttonText, textX, textY);
        font.setColor(Color.WHITE);
    }

    private boolean isHoveringLeftArrow() {
        return touchPoint.x > 20 && touchPoint.x < 100 &&
            touchPoint.y > baseY - 20 && touchPoint.y < baseY + 60;
    }

    private boolean isHoveringRightArrow() {
        return touchPoint.x > VIRTUAL_WIDTH - 100 && touchPoint.x < VIRTUAL_WIDTH - 20 &&
            touchPoint.y > baseY - 20 && touchPoint.y < baseY + 60;
    }

    private void handleInput() {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        touchPoint.set(mouseX, mouseY, 0);
        viewport.unproject(touchPoint);

        isHoveringContinue = btnContinueBounds.contains(touchPoint.x, touchPoint.y);

        if (Gdx.input.justTouched()) {
            // Only allow navigation and selection if NOT already selected
            if (!isSelected) {
                if (isHoveringLeftArrow()) {
                    selectedIndex = (selectedIndex - 1 + totalCharacters) % totalCharacters;
                    animateCarousel(selectedIndex);
                    return;
                }

                if (isHoveringRightArrow()) {
                    selectedIndex = (selectedIndex + 1) % totalCharacters;
                    animateCarousel(selectedIndex);
                    return;
                }

                float centerXPos = currentXOffsets[1];
                float centerYPos = baseY;
                float centerHalfWidth = characterWidth * characterScales[1] / 2f;
                float centerHalfHeight = characterHeight * characterScales[1] / 2f;

                if (touchPoint.x > centerXPos - centerHalfWidth &&
                    touchPoint.x < centerXPos + centerHalfWidth &&
                    touchPoint.y > centerYPos - centerHalfHeight &&
                    touchPoint.y < centerYPos + centerHalfHeight) {
                    isSelected = true;
                    return;
                }
            }

            // PLAY button - only works when selected
            if (isHoveringContinue && isSelected) {
                game.setScreen(new GameScreen(game, characterFolders[selectedIndex], characterNames[selectedIndex]));
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
        font.dispose();
        titleFont.dispose();
        bgTexture.dispose();
        continueButtonTexture.dispose();
        buttonHoverTexture.dispose();
        for (Texture tex : characterTextures) {
            if (tex != null) tex.dispose();
        }
    }
}
