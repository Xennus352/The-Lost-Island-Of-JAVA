package com.xennus352.lostisland.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.xennus352.lostisland.LostIslandGame;

public class CharacterSelectScreen implements Screen {
    private final LostIslandGame game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    // Textures
    private Texture bgTexture;
    private Texture maleCharacterTexture;  // Male character texture
    private Texture femaleCharacterTexture; // Female character texture
    private Texture maleButtonTexture;
    private Texture femaleButtonTexture;
    private Texture continueButtonTexture;
    private Texture buttonHoverTexture;
    private Texture selectionBorderTexture;

    // Font for text
    private BitmapFont font;
    private BitmapFont titleFont;
    private GlyphLayout glyphLayout;

    private OrthographicCamera camera;
    private Viewport viewport;

    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    // Interactive Regions - BIGGER BUTTONS
    private Rectangle btnMaleBounds;
    private Rectangle btnFemaleBounds;
    private Rectangle btnContinueBounds;
    private Vector3 touchPoint;

    // State Tracking
    private String selectedGender = "MALE";
    private boolean isHoveringMale = false;
    private boolean isHoveringFemale = false;
    private boolean isHoveringContinue = false;

    // Animation/Visual States
    private float buttonScale = 1f;
    private float targetScale = 1f;
    private float selectionPulse = 0f;
    private float selectionGlow = 0f;

    public CharacterSelectScreen(LostIslandGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Setup fonts with larger size
        font = new BitmapFont();
        font.getData().setScale(2f);

        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);

        glyphLayout = new GlyphLayout();

        // Load textures
        bgTexture = new Texture("Background/CharacterSelectBg.png");

        // Load separate character textures for male and female
        maleCharacterTexture = new Texture("Fighter/south.png"); // Male character
        femaleCharacterTexture = new Texture("assets/player.png"); // Female character

        // Button textures
        maleButtonTexture = createButtonTexture(Color.BLUE);
        femaleButtonTexture = createButtonTexture(Color.PINK);
        continueButtonTexture = createButtonTexture(Color.GREEN);
        buttonHoverTexture = createButtonTexture(Color.YELLOW);
        selectionBorderTexture = createButtonTexture(new Color(1, 0.8f, 0, 1));

        touchPoint = new Vector3();

        camera = new OrthographicCamera();
        viewport = new ScalingViewport(Scaling.stretch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();

        float scaleFactor = Math.min(
            Gdx.graphics.getWidth() / 800f,
            Gdx.graphics.getHeight() / 600f
        );

        // Position UI elements with BIGGER buttons
        float centerX = VIRTUAL_WIDTH / 2f;
        float buttonWidth = 180f * scaleFactor;
        float buttonHeight = 60f * scaleFactor;
        float buttonSpacing = 30f;

        // Position buttons with more spacing
        btnMaleBounds = new Rectangle(
            centerX - buttonWidth - buttonSpacing - 20,
            180,
            buttonWidth,
            buttonHeight
        );

        btnFemaleBounds = new Rectangle(
            centerX + buttonSpacing + 20,
            180,
            buttonWidth,
            buttonHeight
        );

        // Bigger continue button
        btnContinueBounds = new Rectangle(
            centerX - 120,
            70,
            240,
            55
        );
    }

    private Texture createButtonTexture(Color color) {
        // In practice, load actual button images
        return new Texture("ui/button_default.png");
    }

    @Override
    public void render(float delta) {
        // Update animations
        buttonScale += (targetScale - buttonScale) * 0.1f;
        selectionPulse += delta * 2f;
        selectionGlow = (float) (0.5f + 0.5f * Math.sin(selectionPulse));

        // Update camera
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        // Draw background
        batch.begin();
        batch.draw(bgTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Draw character preview with larger sprites
        drawCharacterSelection(delta);

        // Draw buttons with hover effects
        drawButton(batch, btnMaleBounds, "MALE",
            isHoveringMale ? buttonHoverTexture : maleButtonTexture,
            isHoveringMale ? 1.08f : 1f,
            selectedGender.equals("MALE"));

        drawButton(batch, btnFemaleBounds, "FEMALE",
            isHoveringFemale ? buttonHoverTexture : femaleButtonTexture,
            isHoveringFemale ? 1.08f : 1f,
            selectedGender.equals("FEMALE"));

        drawButton(batch, btnContinueBounds, "CONTINUE →",
            isHoveringContinue ? buttonHoverTexture : continueButtonTexture,
            isHoveringContinue ? 1.08f : 1f,
            false);

        // Draw title with selection info
        drawTitleAndSelection();

        batch.end();

        // Input handling
        handleInput();
    }

    private void drawCharacterSelection(float delta) {
        float spriteWidth = 100f;
        float spriteHeight = 100f;
        float pedestalY = 145f;

        // Draw male character (left)
        float leftPedestalX = 256f - (spriteWidth / 2f);
        drawCharacterWithSelection(leftPedestalX, pedestalY, spriteWidth, spriteHeight,
            selectedGender.equals("MALE"), "MALE", maleCharacterTexture);

        // Draw female character (right)
        float rightPedestalX = 384f - (spriteWidth / 2f);
        drawCharacterWithSelection(rightPedestalX, pedestalY, spriteWidth, spriteHeight,
            selectedGender.equals("FEMALE"), "FEMALE", femaleCharacterTexture);
    }

    private void drawCharacterWithSelection(float x, float y, float width, float height,
                                            boolean isSelected, String gender, Texture characterTexture) {
        // Draw character sprite
        batch.draw(characterTexture, x, y, width, height);

        if (isSelected) {
            // Draw glowing selection border
            float borderSize = 12f;
            float glowAlpha = 0.3f + 0.3f * selectionGlow;

            // Outer glow
            batch.setColor(1, 0.8f, 0, glowAlpha * 0.5f);
            batch.draw(selectionBorderTexture,
                x - borderSize, y - borderSize,
                width + borderSize * 2, height + borderSize * 2);

            // Inner glow
            batch.setColor(1, 0.9f, 0.3f, glowAlpha);
            batch.draw(selectionBorderTexture,
                x - borderSize/2, y - borderSize/2,
                width + borderSize, height + borderSize);

            batch.setColor(Color.WHITE);

            // Draw "SELECTED" label
            String label = "★ SELECTED ★";
            glyphLayout.setText(font, label);
            float labelX = x + (width - glyphLayout.width) / 2f;
            float labelY = y + height + 35f;

            // Label background
            batch.setColor(0, 0, 0, 0.6f);
            batch.draw(selectionBorderTexture,
                labelX - 15, labelY - 8,
                glyphLayout.width + 30, glyphLayout.height + 16);

            // Label text
            batch.setColor(1, 0.8f, 0, 1);
            font.draw(batch, label, labelX, labelY + glyphLayout.height);
            batch.setColor(Color.WHITE);
        }

        // Draw gender label under character with appropriate color
        String genderLabel = gender.equals("MALE") ? "♂ MALE" : "♀ FEMALE";
        glyphLayout.setText(font, genderLabel);
        float labelX = x + (width - glyphLayout.width) / 2f;
        float labelY = y - 20f;

        // Color the label based on gender
        if (gender.equals("MALE")) {
            batch.setColor(new Color(0.3f, 0.6f, 1f, 1f)); // Blue for male
        } else {
            batch.setColor(new Color(1f, 0.4f, 0.7f, 1f)); // Pink for female
        }
        font.draw(batch, genderLabel, labelX, labelY);
        batch.setColor(Color.WHITE);
    }

    private void drawButton(SpriteBatch batch, Rectangle bounds, String text,
                            Texture texture, float scale, boolean isSelected) {
        float originalX = bounds.x;
        float originalY = bounds.y;
        float originalWidth = bounds.width;
        float originalHeight = bounds.height;

        // Apply scale effect
        float scaledWidth = originalWidth * scale;
        float scaledHeight = originalHeight * scale;
        float x = originalX + (originalWidth - scaledWidth) / 2f;
        float y = originalY + (originalHeight - scaledHeight) / 2f;

        // Draw button with selection state
        if (isSelected) {
            batch.setColor(1, 0.8f, 0, 1);
        } else {
            batch.setColor(Color.WHITE);
        }

        if (texture != null) {
            batch.draw(texture, x, y, scaledWidth, scaledHeight);
        }

        // Draw button text with shadow for better visibility
        if (isSelected) {
            batch.setColor(Color.BLACK);
        } else {
            batch.setColor(Color.BLACK);
            // Draw shadow
            glyphLayout.setText(font, text);
            font.draw(batch, text,
                bounds.x + (bounds.width - glyphLayout.width) / 2f + 2,
                bounds.y + (bounds.height + glyphLayout.height) / 2f - 2);

            batch.setColor(Color.WHITE);
        }

        // Draw main text
        glyphLayout.setText(font, text);
        float textX = bounds.x + (bounds.width - glyphLayout.width) / 2f;
        float textY = bounds.y + (bounds.height + glyphLayout.height) / 2f;

        // Add checkmark for selected button
        String displayText = isSelected ? "✓ " + text : text;
        glyphLayout.setText(font, displayText);

        if (isSelected) {
            batch.setColor(Color.BLACK);
        } else {
            batch.setColor(Color.WHITE);
        }
        font.draw(batch, displayText, textX, textY);

        batch.setColor(Color.WHITE);
    }

    private void drawTitleAndSelection() {
        // Main title
        titleFont.setColor(Color.WHITE);
        String title = "SELECT YOUR CHARACTER";
        glyphLayout.setText(titleFont, title);
        float titleX = (VIRTUAL_WIDTH - glyphLayout.width) / 2f;
        titleFont.draw(batch, title, titleX, VIRTUAL_HEIGHT - 30);

        // Selection status
        String selectedText = "Selected: " + selectedGender +
            (selectedGender.equals("MALE") ? " ♂" : " ♀");
        glyphLayout.setText(font, selectedText);
        float statusX = (VIRTUAL_WIDTH - glyphLayout.width) / 2f;
        float statusY = 310f;

        // Status background with more padding
        batch.setColor(0, 0, 0, 0.4f);
        float padding = 25f;
        batch.draw(selectionBorderTexture,
            statusX - padding, statusY - 10,
            glyphLayout.width + padding * 2, glyphLayout.height + 20);

        // Status text with color
        batch.setColor(selectedGender.equals("MALE") ?
            new Color(0.3f, 0.5f, 1f, 1f) :
            new Color(1f, 0.4f, 0.6f, 1f));
        font.draw(batch, selectedText, statusX, statusY + glyphLayout.height);
        batch.setColor(Color.WHITE);

        // Add instruction text
        String instruction = "Tap a character to select, then CONTINUE";
        glyphLayout.setText(font, instruction);
        float instX = (VIRTUAL_WIDTH - glyphLayout.width) / 2f;
        float instY = 120f;
        batch.setColor(0.5f, 0.5f, 0.5f, 0.8f);
        font.draw(batch, instruction, instX, instY);
        batch.setColor(Color.WHITE);
    }

    private void handleInput() {
        // Mouse/Touch position for hover detection
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        touchPoint.set(mouseX, mouseY, 0);
        viewport.unproject(touchPoint);

        // Update hover states
        isHoveringMale = btnMaleBounds.contains(touchPoint.x, touchPoint.y);
        isHoveringFemale = btnFemaleBounds.contains(touchPoint.x, touchPoint.y);
        isHoveringContinue = btnContinueBounds.contains(touchPoint.x, touchPoint.y);

        // Click handling
        if (Gdx.input.justTouched()) {
            if (isHoveringMale) {
                selectedGender = "MALE";
                targetScale = 0.85f;
                Gdx.app.log("CharacterSelect", "Male Chosen");
            } else if (isHoveringFemale) {
                selectedGender = "FEMALE";
                targetScale = 0.85f;
                Gdx.app.log("CharacterSelect", "Female Chosen");
            } else if (isHoveringContinue) {
                Gdx.app.log("CharacterSelect", "Continuing with: " + selectedGender);
                game.setScreen(new GameScreen(game, selectedGender));
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
        shapeRenderer.dispose();
        font.dispose();
        titleFont.dispose();
        bgTexture.dispose();
        if (maleCharacterTexture != null) maleCharacterTexture.dispose();
        if (femaleCharacterTexture != null) femaleCharacterTexture.dispose();
        if (maleButtonTexture != null) maleButtonTexture.dispose();
        if (femaleButtonTexture != null) femaleButtonTexture.dispose();
        if (continueButtonTexture != null) continueButtonTexture.dispose();
        if (buttonHoverTexture != null) buttonHoverTexture.dispose();
        if (selectionBorderTexture != null) selectionBorderTexture.dispose();
    }
}
