package com.xennus352.lostisland.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.xennus352.lostisland.LostIslandGame;
import com.xennus352.lostisland.entity.NPC;
import com.xennus352.lostisland.entity.Player;

public class GameScreen implements Screen {

    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 600f;

    private final LostIslandGame game;
    private final String characterPath;
    private final String characterName;

    private SpriteBatch batch;
    private Player player;
    private NPC healerNpc;
    private boolean wasPlayerAttacking = false;
    private boolean isGameOver = false;

    private OrthographicCamera camera;
    private Viewport viewport;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private int mapPixelWidth;
    private int mapPixelHeight;

    private ShapeRenderer debugRenderer;
    private ShapeRenderer hudRenderer;
    private BitmapFont hudFont;

    private FrameBuffer fbo;
    private TextureRegion fboRegion;
    private OrthographicCamera screenCam;

    private static final String[] SOLID_LAYERS = {"Tree", "House"};
    private static final float ZOOM = 0.8f;

    private Array<FloatingText> floatingTexts = new Array<>();
    private GlyphLayout textLayout = new GlyphLayout();

    private boolean dialogueOpen = false;
    private int dialoguePage = 0;
    private String[] dialogueLines = null;
    private NPC dialogueNpc = null;

    private boolean showMenu = false;
    private boolean clickConsumed = false;

    private static class FloatingText {
        String text;
        float x, y;
        float timer;
        Color color;
        FloatingText(String text, float x, float y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.timer = 1.2f;
            this.color = color;
        }
    }

    public GameScreen(LostIslandGame game, String characterPath, String characterName) {
        this.game = game;
        this.characterPath = characterPath;
        this.characterName = characterName;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();

        map = new TmxMapLoader().load("Maps/Village.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);

        debugRenderer = new ShapeRenderer();
        hudRenderer = new ShapeRenderer();
        hudFont = new BitmapFont();

        screenCam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        screenCam.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0);
        screenCam.update();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);

        player = new Player(400, 300, characterPath);

        mapPixelWidth = map.getProperties().get("width", Integer.class) * 32;
        mapPixelHeight = map.getProperties().get("height", Integer.class) * 32;
        player.setWorldBounds(mapPixelWidth, mapPixelHeight);

        camera.zoom = ZOOM;

        TiledMapTileLayer streetLayer = (TiledMapTileLayer) map.getLayers().get("Street");
        System.out.println("[DEBUG] Street layer found: " + (streetLayer != null));

        Player.CollisionChecker collisionChecker = (tileX, tileY) -> {
            if (streetLayer != null) {
                TiledMapTileLayer.Cell streetCell = streetLayer.getCell(tileX, tileY);
                if (streetCell != null && streetCell.getTile() != null) return false;
            }
            for (String layerName : SOLID_LAYERS) {
                TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerName);
                if (layer == null) continue;
                TiledMapTileLayer.Cell cell = layer.getCell(tileX, tileY);
                if (cell == null || cell.getTile() == null) continue;
                if (layerName.equals("House")) {
                    int gid = cell.getTile().getId();
                    if (gid >= 26 && gid <= 34) return true;
                } else {
                    return true;
                }
            }
            return false;
        };
        player.setCollisionChecker(collisionChecker);

        final String[] slowLayers = {"Stream", "River"};
        player.setTileSpeedModifier((tileX, tileY) -> {
            for (String name : slowLayers) {
                TiledMapTileLayer l = (TiledMapTileLayer) map.getLayers().get(name);
                if (l == null) continue;
                TiledMapTileLayer.Cell c = l.getCell(tileX, tileY);
                if (c != null && c.getTile() != null) return 0.4f;
            }
            TiledMapTileLayer houseLayer = (TiledMapTileLayer) map.getLayers().get("House");
            if (houseLayer != null) {
                TiledMapTileLayer.Cell c = houseLayer.getCell(tileX, tileY);
                if (c != null && c.getTile() != null) {
                    int gid = c.getTile().getId();
                    if (gid >= 35 && gid <= 46) return 0.4f;
                }
            }
            return 1f;
        });

        findSafeSpawn();

        healerNpc = new NPC(player.getX() + 100, player.getY() + 50, "Healer", player, "Healer");
        healerNpc.setCollisionChecker(collisionChecker);
        healerNpc.setWorldBounds(mapPixelWidth, mapPixelHeight);
        healerNpc.setDialogue(new String[]{
            "Greetings, traveler.",
            "I am the healer of this village.",
            "Beware of the dangers lurking outside.",
            "May the light guide your path."
        });
        findSafeNpcSpawn();

        System.out.println("Selected Character : " + characterName);
    }

    private void findSafeSpawn() {
        TiledMapTileLayer ground = (TiledMapTileLayer) map.getLayers().get("Ground");
        if (ground == null) return;

        int tx = (int)Math.floor(player.getX() / 32);
        int ty = (int)Math.floor(player.getY() / 32);
        if (!isTileBlocked(tx, ty) && isAreaClear(tx, ty, 3)) return;

        int bestX = -1, bestY = -1, bestScore = -1;
        for (int y = 0; y < ground.getHeight(); y++) {
            for (int x = 0; x < ground.getWidth(); x++) {
                if (ground.getCell(x, y) == null || ground.getCell(x, y).getTile() == null)
                    continue;
                if (isTileBlocked(x, y)) continue;
                int score = 0;
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= ground.getWidth() || ny < 0 || ny >= ground.getHeight())
                            continue;
                        if (ground.getCell(nx, ny) == null || ground.getCell(nx, ny).getTile() == null)
                            continue;
                        if (!isTileBlocked(nx, ny)) score++;
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        if (bestX >= 0 && bestY >= 0)
            player.setPosition(bestX * 32, bestY * 32);
    }

    private boolean isAreaClear(int cx, int cy, int radius) {
        TiledMapTileLayer ground = (TiledMapTileLayer) map.getLayers().get("Ground");
        if (ground == null) return false;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int nx = cx + dx, ny = cy + dy;
                if (nx < 0 || nx >= ground.getWidth() || ny < 0 || ny >= ground.getHeight())
                    return false;
                if (ground.getCell(nx, ny) == null || ground.getCell(nx, ny).getTile() == null)
                    return false;
                if (isTileBlocked(nx, ny)) return false;
            }
        }
        return true;
    }

    private void findSafeNpcSpawn() {
        int tx = (int)Math.floor(healerNpc.getX() / 32);
        int ty = (int)Math.floor(healerNpc.getY() / 32);
        if (!isTileBlocked(tx, ty)) return;

        TiledMapTileLayer ground = (TiledMapTileLayer) map.getLayers().get("Ground");
        if (ground == null) return;

        for (int y = 0; y < ground.getHeight(); y++) {
            for (int x = 0; x < ground.getWidth(); x++) {
                if (ground.getCell(x, y) == null || ground.getCell(x, y).getTile() == null)
                    continue;
                if (isTileBlocked(x, y)) continue;
                healerNpc.setPosition(x * 32, y * 32);
                return;
            }
        }
    }

    private boolean isTileBlocked(int tileX, int tileY) {
        TiledMapTileLayer street = (TiledMapTileLayer) map.getLayers().get("Street");
        if (street != null) {
            TiledMapTileLayer.Cell sc = street.getCell(tileX, tileY);
            if (sc != null && sc.getTile() != null) return false;
        }
        for (String layerName : SOLID_LAYERS) {
            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerName);
            if (layer == null) continue;
            TiledMapTileLayer.Cell cell = layer.getCell(tileX, tileY);
            if (cell == null || cell.getTile() == null) continue;
            if (layerName.equals("House")) {
                int gid = cell.getTile().getId();
                if (gid >= 26 && gid <= 34) return true;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(float delta) {
        clickConsumed = false;

        if (!isGameOver && !dialogueOpen) {
            player.update(delta);
            healerNpc.update(delta);

            if (player.getHealth() <= 0) {
                isGameOver = true;
            }
        } else if (dialogueOpen && !isGameOver) {
            healerNpc.update(delta);
            if (healerNpc.isHostile() || !healerNpc.isAlive()) dialogueOpen = false;
        }

        float playerCenterX = player.getX() + (player.getWidth() / 2f);
        float playerCenterY = player.getY() + (player.getHeight() / 2f);

        float halfViewW = (camera.viewportWidth * camera.zoom) / 2f;
        float halfViewH = (camera.viewportHeight * camera.zoom) / 2f;
        float camX = MathUtils.clamp(playerCenterX, halfViewW, mapPixelWidth - halfViewW);
        float camY = MathUtils.clamp(playerCenterY, halfViewH, mapPixelHeight - halfViewH);
        camera.position.set(camX, camY, 0);
        camera.update();

        if (!isGameOver) {
            if (player.isAttacking() && !wasPlayerAttacking) {
                if (isNpcInPlayerAttackRange()) {
                    int dmg = player.getAttackDamage();
                    boolean killed = healerNpc.takeDamage(dmg);
                    float nx = healerNpc.getX() + healerNpc.getWidth() * 0.3f;
                    float ny = healerNpc.getY() + healerNpc.getHeight() * 0.6f;
                    floatingTexts.add(new FloatingText("-" + dmg, nx, ny, Color.RED));
                    if (killed) {
                        player.addExp(30);
                        floatingTexts.add(new FloatingText("+30 EXP", nx + 15, ny + 10, Color.YELLOW));
                    }
                }
            }
            wasPlayerAttacking = player.isAttacking();

            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                if (dialogueOpen) {
                    dialogueOpen = false;
                } else if (isPlayerNearNpc() && healerNpc.isAlive() && !healerNpc.isHostile()) {
                    dialogueOpen = true;
                    dialoguePage = 0;
                    dialogueLines = healerNpc.getDialogue();
                    dialogueNpc = healerNpc;
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                showMenu = true;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                if (dialogueOpen) dialogueOpen = false;
                else showMenu = !showMenu;
            }

            for (int i = floatingTexts.size - 1; i >= 0; i--) {
                floatingTexts.get(i).timer -= delta;
                if (floatingTexts.get(i).timer <= 0) floatingTexts.removeIndex(i);
            }
        }

        boolean overlay = dialogueOpen || isGameOver || showMenu;

        if (overlay) {
            int w = Gdx.graphics.getWidth();
            int h = Gdx.graphics.getHeight();
            if (fbo.getWidth() != w || fbo.getHeight() != h) {
                fbo.dispose();
                fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
                fboRegion = new TextureRegion(fbo.getColorBufferTexture());
                fboRegion.flip(false, true);
                screenCam = new OrthographicCamera(w, h);
                screenCam.position.set(w / 2f, h / 2f, 0);
                screenCam.update();
            }
            fbo.begin();
        }

        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1);

        mapRenderer.setView(camera);
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.draw(batch);
        healerNpc.draw(batch);
        healerNpc.drawName(batch, hudFont);

        for (FloatingText ft : floatingTexts) {
            float alpha = Math.max(0, ft.timer / 1.2f);
            float rise = (1f - alpha) * 25f;
            hudFont.setColor(ft.color.r, ft.color.g, ft.color.b, alpha);
            textLayout.setText(hudFont, ft.text);
            hudFont.draw(batch, ft.text, ft.x - textLayout.width / 2f, ft.y + rise);
        }
        batch.end();

        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Filled);
        healerNpc.drawHpBar(debugRenderer);
        debugRenderer.end();

        drawCollisionDebug();

        if (overlay) {
            fbo.end();

            int w = Gdx.graphics.getWidth();
            int h = Gdx.graphics.getHeight();
            ScreenUtils.clear(0, 0, 0, 1);

            screenCam = new OrthographicCamera(w, h);
            screenCam.position.set(w / 2f, h / 2f, 0);
            screenCam.update();

            batch.setProjectionMatrix(screenCam.combined);
            batch.begin();
            batch.setColor(1, 1, 1, 0.1f);
            for (int ox = -2; ox <= 2; ox++) {
                for (int oy = -2; oy <= 2; oy++) {
                    batch.draw(fboRegion, ox, oy, w, h);
                }
            }
            batch.setColor(0.15f, 0.15f, 0.2f, 0.55f);
            batch.draw(fboRegion, 0, 0, w, h);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        drawHUD();

        if (dialogueOpen) drawDialogue();
        if (showMenu && !isGameOver && !dialogueOpen) drawPauseMenu();
        if (isGameOver) drawGameOver();
    }

    private void drawGameOver() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        OrthographicCamera hudCam = new OrthographicCamera(screenW, screenH);
        hudCam.position.set(screenW / 2f, screenH / 2f, 0);
        hudCam.update();

        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        hudFont.setColor(1, 0.2f, 0.2f, 1);
        String title = "GAME OVER";
        textLayout.setText(hudFont, title);
        hudFont.draw(batch, title, (screenW - textLayout.width) / 2f, screenH / 2f + 40);

        hudFont.setColor(0.8f, 0.8f, 0.8f, 1);
        String sub1 = "Level " + player.getLevel() + "  |  EXP " + player.getExp() + "/" + player.getExpToNext();
        textLayout.setText(hudFont, sub1);
        hudFont.draw(batch, sub1, (screenW - textLayout.width) / 2f, screenH / 2f);

        hudFont.setColor(0.6f, 0.6f, 0.6f, 1);
        String sub2 = "Press [R] to Restart  |  [M] for Menu";
        textLayout.setText(hudFont, sub2);
        hudFont.draw(batch, sub2, (screenW - textLayout.width) / 2f, screenH / 2f - 30);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            game.setScreen(new GameScreen(game, characterPath, characterName));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    private void drawDialogue() {
        if (dialogueLines == null || dialogueNpc == null) return;

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        OrthographicCamera hudCam = new OrthographicCamera(screenW, screenH);
        hudCam.position.set(screenW / 2f, screenH / 2f, 0);
        hudCam.update();

        float panelW = 520;
        float panelH = 180;
        float panelX = (screenW - panelW) / 2f;
        float panelY = 40;
        float pad = 12;
        float spriteSize = 80;

        hudRenderer.setProjectionMatrix(hudCam.combined);
        hudRenderer.begin(ShapeRenderer.ShapeType.Filled);
        hudRenderer.setColor(0.08f, 0.08f, 0.12f, 0.9f);
        hudRenderer.rect(panelX, panelY, panelW, panelH);
        hudRenderer.end();

        hudRenderer.begin(ShapeRenderer.ShapeType.Line);
        hudRenderer.setColor(0.4f, 0.4f, 0.5f, 1);
        hudRenderer.rect(panelX, panelY, panelW, panelH);
        hudRenderer.end();

        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();

        float spriteX = panelX + pad;
        float spriteY = panelY + pad + (panelH - pad * 2 - spriteSize) / 2f;
        TextureRegion frame = dialogueNpc.getCurrentFrame();
        if (frame != null) {
            batch.draw(frame, spriteX, spriteY, spriteSize, spriteSize);
        }

        float textX = spriteX + spriteSize + pad * 2;
        float textY = panelY + panelH - pad - 8;
        hudFont.setColor(1, 0.85f, 0.4f, 1);
        hudFont.draw(batch, "Healer", textX, textY);

        float speechY = textY - 24;
        hudFont.setColor(0.95f, 0.95f, 0.95f, 1);
        String pageText = dialoguePage < dialogueLines.length ? dialogueLines[dialoguePage] : "";
        textLayout.setText(hudFont, pageText);
        float wrapWidth = panelW - (textX - panelX) - pad;
        if (textLayout.width > wrapWidth) {
            String[] words = pageText.split(" ");
            StringBuilder line1 = new StringBuilder();
            StringBuilder line2 = new StringBuilder();
            boolean second = false;
            for (String w : words) {
                StringBuilder test = new StringBuilder(second ? line2 : line1);
                if (test.length() > 0) test.append(" ");
                test.append(w);
                textLayout.setText(hudFont, test.toString());
                if (textLayout.width > wrapWidth) {
                    if (!second) { second = true; line2.append(w); }
                } else {
                    if (second) line2.append(" ").append(w);
                    else { if (line1.length() > 0) line1.append(" "); line1.append(w); }
                }
            }
            hudFont.draw(batch, line1.toString(), textX, speechY);
            if (line2.length() > 0) hudFont.draw(batch, line2.toString(), textX, speechY - 20);
        } else {
            hudFont.draw(batch, pageText, textX, speechY);
        }

        float closeBtnX = panelX + panelW - 28;
        float closeBtnY = panelY + panelH - 28;
        float closeBtnSize = 20;
        hudFont.setColor(0.8f, 0.3f, 0.3f, 1);
        hudFont.draw(batch, "X", closeBtnX + 4, closeBtnY + 15);

        if (!clickConsumed && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = screenH - Gdx.input.getY();

            if (mx >= closeBtnX && mx <= closeBtnX + closeBtnSize && my >= closeBtnY && my <= closeBtnY + closeBtnSize) {
                dialogueOpen = false;
            } else if (dialoguePage < dialogueLines.length - 1) {
                float nextBtnX = panelX + panelW - 80;
                float nextBtnY = panelY + 10;
                if (mx >= nextBtnX && mx <= nextBtnX + 70 && my >= nextBtnY && my <= nextBtnY + 20) {
                    dialoguePage++;
                }
            }
            clickConsumed = true;
        }

        if (dialoguePage < dialogueLines.length - 1) {
            float nextBtnX = panelX + panelW - 80;
            float nextBtnY = panelY + 10;
            hudFont.setColor(0.3f, 0.6f, 1f, 1);
            hudFont.draw(batch, "[Next >>]", nextBtnX, nextBtnY + 14);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (dialoguePage < dialogueLines.length - 1) {
                dialoguePage++;
            } else {
                dialogueOpen = false;
            }
        }

        batch.end();
    }

    private void drawPauseMenu() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        OrthographicCamera hudCam = new OrthographicCamera(screenW, screenH);
        hudCam.position.set(screenW / 2f, screenH / 2f, 0);
        hudCam.update();

        float panelW = 240;
        float panelH = 180;
        float panelX = (screenW - panelW) / 2f;
        float panelY = (screenH - panelH) / 2f;

        hudRenderer.setProjectionMatrix(hudCam.combined);
        hudRenderer.begin(ShapeRenderer.ShapeType.Filled);
        hudRenderer.setColor(0.1f, 0.1f, 0.14f, 0.92f);
        hudRenderer.rect(panelX, panelY, panelW, panelH);
        hudRenderer.end();
        hudRenderer.begin(ShapeRenderer.ShapeType.Line);
        hudRenderer.setColor(0.4f, 0.4f, 0.5f, 1);
        hudRenderer.rect(panelX, panelY, panelW, panelH);
        hudRenderer.end();

        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();

        hudFont.setColor(1, 1, 1, 1);
        textLayout.setText(hudFont, "PAUSED");
        hudFont.draw(batch, "PAUSED", (screenW - textLayout.width) / 2f, panelY + panelH - 18);

        float btnW = 160;
        float btnH = 30;
        float btnX = (screenW - btnW) / 2f;
        float labelX = btnX + 10;
        float restY = panelY + panelH - 56;
        float menuY = restY - 46;
        float closeY = menuY - 46;

        hudFont.setColor(0.9f, 0.9f, 0.9f, 1);
        hudFont.draw(batch, "[R] Restart", labelX, restY + 20);
        hudFont.draw(batch, "[M] Main Menu", labelX, menuY + 20);
        hudFont.setColor(0.5f, 0.5f, 0.5f, 1);
        hudFont.draw(batch, "[Esc] Close", labelX, closeY + 20);

        batch.end();

        if (!clickConsumed && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = screenH - Gdx.input.getY();
            if (mx >= btnX && mx <= btnX + btnW && my >= restY && my <= restY + btnH) {
                game.setScreen(new GameScreen(game, characterPath, characterName));
            } else if (mx >= btnX && mx <= btnX + btnW && my >= menuY && my <= menuY + btnH) {
                game.setScreen(new MainMenuScreen(game));
            } else if (mx >= btnX && mx <= btnX + btnW && my >= closeY && my <= closeY + btnH) {
                showMenu = false;
            } else if (!(mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH)) {
                showMenu = false;
            }
            clickConsumed = true;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            game.setScreen(new GameScreen(game, characterPath, characterName));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            game.setScreen(new MainMenuScreen(game));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            showMenu = false;
        }
    }

    private boolean isNpcInPlayerAttackRange() {
        if (healerNpc.isDead()) return false;
        float px = player.getX() + player.getWidth() * 0.3f;
        float py = player.getY() + player.getHeight() * 0.3f;
        float nx = healerNpc.getX() + healerNpc.getWidth() * 0.3f;
        float ny = healerNpc.getY() + healerNpc.getHeight() * 0.3f;
        float dx = nx - px;
        float dy = ny - py;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        return dist < 40f;
    }

    private boolean isPlayerNearNpc() {
        if (healerNpc == null || healerNpc.isDead()) return false;
        float px = player.getX() + player.getWidth() * 0.3f;
        float py = player.getY() + player.getHeight() * 0.3f;
        float nx = healerNpc.getX() + healerNpc.getWidth() * 0.3f;
        float ny = healerNpc.getY() + healerNpc.getHeight() * 0.3f;
        float dx = nx - px;
        float dy = ny - py;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        return dist < 50f;
    }

    private void drawCollisionDebug() {
        debugRenderer.setProjectionMatrix(camera.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        debugRenderer.setColor(Color.RED);

        TiledMapTileLayer street = (TiledMapTileLayer) map.getLayers().get("Street");
        for (String layerName : SOLID_LAYERS) {
            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerName);
            if (layer == null) continue;
            for (int tx = 0; tx < layer.getWidth(); tx++) {
                for (int ty = 0; ty < layer.getHeight(); ty++) {
                    if (street != null) {
                        TiledMapTileLayer.Cell sc = street.getCell(tx, ty);
                        if (sc != null && sc.getTile() != null) continue;
                    }
                    TiledMapTileLayer.Cell cell = layer.getCell(tx, ty);
                    if (cell != null && cell.getTile() != null) {
                        if (layerName.equals("House")) {
                            int gid = cell.getTile().getId();
                            if (gid < 26 || gid > 34) continue;
                        }
                        debugRenderer.rect(tx * 32, ty * 32, 32, 32);
                    }
                }
            }
        }

        debugRenderer.end();
    }

    private void drawHUD() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        OrthographicCamera hudCam = new OrthographicCamera(screenW, screenH);
        hudCam.position.set(screenW / 2f, screenH / 2f, 0);
        hudCam.update();

        hudRenderer.setProjectionMatrix(hudCam.combined);

        int margin = 16;
        int padding = 8;
        float barW = 180, barH = 16;
        float labelW = 50;
        float rightPad = 10;
        float panelX = margin;
        float panelW = margin + labelW + padding + barW + padding + rightPad;
        float innerH = barH * 2 + padding * 2 + 20;
        float panelY = screenH - margin - innerH;

        hudRenderer.begin(ShapeRenderer.ShapeType.Filled);
        hudRenderer.setColor(0, 0, 0, 0.55f);
        hudRenderer.rect(panelX, panelY, panelW, innerH);
        hudRenderer.end();

        hudRenderer.begin(ShapeRenderer.ShapeType.Line);
        hudRenderer.setColor(0.5f, 0.5f, 0.5f, 0.8f);
        hudRenderer.rect(panelX, panelY, panelW, innerH);
        hudRenderer.end();

        float labelX = panelX + margin;
        float barX  = labelX + labelW;
        float contentY = panelY + padding + 10;

        float hpY = contentY + barH + padding;
        float hpFill = (float)player.getHealth() / player.getMaxHealth();

        hudRenderer.begin(ShapeRenderer.ShapeType.Filled);
        hudRenderer.setColor(0.15f, 0.15f, 0.15f, 1);
        hudRenderer.rect(barX, hpY, barW, barH);

        float hpR = 0.8f + 0.2f * (1 - hpFill);
        float hpG = 0.2f * hpFill;
        hudRenderer.setColor(hpR, hpG, 0.1f, 1);
        hudRenderer.rect(barX, hpY, barW * hpFill, barH);
        hudRenderer.end();

        hudRenderer.begin(ShapeRenderer.ShapeType.Line);
        hudRenderer.setColor(0.4f, 0.4f, 0.4f, 1);
        hudRenderer.rect(barX, hpY, barW, barH);
        hudRenderer.end();

        float expY = contentY;
        float expFill = (float)player.getExp() / Math.max(player.getExpToNext(), 1);

        hudRenderer.begin(ShapeRenderer.ShapeType.Filled);
        hudRenderer.setColor(0.15f, 0.15f, 0.15f, 1);
        hudRenderer.rect(barX, expY, barW, barH);

        float expR = 0.2f, expG = 0.3f + 0.5f * expFill, expB = 0.8f;
        hudRenderer.setColor(expR, expG, expB, 1);
        hudRenderer.rect(barX, expY, barW * expFill, barH);
        hudRenderer.end();

        hudRenderer.begin(ShapeRenderer.ShapeType.Line);
        hudRenderer.setColor(0.4f, 0.4f, 0.4f, 1);
        hudRenderer.rect(barX, expY, barW, barH);
        hudRenderer.end();

        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        hudFont.setColor(0.95f, 0.95f, 0.95f, 1);

        float textY = hpY + (barH + 4);
        hudFont.draw(batch, "HP", labelX, textY);
        hudFont.draw(batch, player.getHealth() + "/" + player.getMaxHealth(), barX + barW - 50, textY);

        textY = expY + (barH + 4);
        hudFont.draw(batch, "EXP", labelX, textY);
        hudFont.draw(batch, player.getExp() + "/" + player.getExpToNext(), barX + barW - 50, textY);

        hudFont.setColor(1, 0.85f, 0.2f, 1);
        hudFont.draw(batch, "Lv." + player.getLevel(), barX, expY - 2);

        batch.end();

        float btnW = 56;
        float btnH = 30;
        float btnX = screenW - btnW - 10;
        float btnY = screenH - btnH - 10;
        hudRenderer.setProjectionMatrix(hudCam.combined);
        hudRenderer.begin(ShapeRenderer.ShapeType.Filled);
        hudRenderer.setColor(0.15f, 0.15f, 0.2f, 0.7f);
        hudRenderer.rect(btnX, btnY, btnW, btnH);
        hudRenderer.end();
        hudRenderer.begin(ShapeRenderer.ShapeType.Line);
        hudRenderer.setColor(0.4f, 0.4f, 0.5f, 0.6f);
        hudRenderer.rect(btnX, btnY, btnW, btnH);
        hudRenderer.end();

        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        hudFont.setColor(0.8f, 0.8f, 0.8f, 1);
        textLayout.setText(hudFont, "MENU");
        hudFont.draw(batch, "MENU", btnX + (btnW - textLayout.width) / 2f, btnY + 20);

        batch.end();

        if (!clickConsumed && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = screenH - Gdx.input.getY();
            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                showMenu = !showMenu;
                clickConsumed = true;
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
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
        if (batch != null) batch.dispose();
        if (player != null) player.dispose();
        if (healerNpc != null) healerNpc.dispose();
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (debugRenderer != null) debugRenderer.dispose();
        if (hudRenderer != null) hudRenderer.dispose();
        if (hudFont != null) hudFont.dispose();
        if (fbo != null) fbo.dispose();
    }
}
