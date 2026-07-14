package com.xennus352.lostisland.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.xennus352.lostisland.LostIslandGame;
import com.xennus352.lostisland.entity.Player;

public class GameScreen implements Screen {

    // 🎥 ဂိမ်းရဲ့ ကင်မရာ view size
    private static final float WORLD_WIDTH = 800f;
    private static final float WORLD_HEIGHT = 600f;

    private final LostIslandGame game;
    private final String characterPath;
    private final String characterName;

    private SpriteBatch batch;
    private Player player;

    private OrthographicCamera camera;
    private Viewport viewport;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private int mapPixelWidth;
    private int mapPixelHeight;

    private ShapeRenderer debugRenderer;
    private ShapeRenderer hudRenderer;
    private BitmapFont hudFont;

    private static final String[] SOLID_LAYERS = {"Tree", "House"};
    private static final float ZOOM = 0.8f;

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

        // 🗺️ updated to load Village.tmx instead of island.tmx
        map = new TmxMapLoader().load("Maps/Village.tmx");

        // မြေပုံအကွက်တွေ အချိုးအစားမပျက်စေဖို့ unitScale ကို 1f ထားပါတယ်
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);

        debugRenderer = new ShapeRenderer();
        hudRenderer = new ShapeRenderer();
        hudFont = new BitmapFont();

        // Player ကို စတင်မယ့်နေရာမှာ နေရာချခြင်း
        player = new Player(400, 300, characterPath);

        // Store map dimensions for camera clamping
        mapPixelWidth = map.getProperties().get("width", Integer.class) * 32;
        mapPixelHeight = map.getProperties().get("height", Integer.class) * 32;
        player.setWorldBounds(mapPixelWidth, mapPixelHeight);

        camera.zoom = ZOOM;

        // Set up collision detection (tile-coordinate based)
        // Street tiles override solid layers — roads are always walkable
        // House layer: only block actual houses (GID 26–34, House.tsx / VillageHouse.png),
        //              not bridge/filler tiles (GID 35–46, House1.tsx / VillageHouse2.png)
        TiledMapTileLayer streetLayer = (TiledMapTileLayer) map.getLayers().get("Street");
        System.out.println("[DEBUG] Street layer found: " + (streetLayer != null));
        player.setCollisionChecker((tileX, tileY) -> {
            if (streetLayer != null) {
                TiledMapTileLayer.Cell streetCell = streetLayer.getCell(tileX, tileY);
                if (streetCell != null && streetCell.getTile() != null) return false;
            }
            for (String layerName : SOLID_LAYERS) {
                TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(layerName);
                if (layer == null) continue;
                TiledMapTileLayer.Cell cell = layer.getCell(tileX, tileY);
                if (cell == null || cell.getTile() == null) continue;
                // On the House layer only actual houses (GID 26–34) block
                if (layerName.equals("House")) {
                    int gid = cell.getTile().getId();
                    if (gid >= 26 && gid <= 34) return true;
                } else {
                    return true;
                }
            }
            return false;
        });

        // Slow down on Stream / River tiles, and on bridge tiles (House1, GID 35–46)
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

        // Move player to a safe spawn if current position is blocked
        findSafeSpawn();

        System.out.println("Selected Character : " + characterName);
    }

    private void findSafeSpawn() {
        TiledMapTileLayer ground = (TiledMapTileLayer) map.getLayers().get("Ground");
        if (ground == null) return;

        // First check if current spawn (400, 300) is already safe
        int tx = (int)Math.floor(player.getX() / 32);
        int ty = (int)Math.floor(player.getY() / 32);
        if (!isTileBlocked(tx, ty) && isAreaClear(tx, ty, 3)) return;

        // Find the most open area (most clear tiles in a 5x5 radius)
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

    private boolean isTileBlocked(int tileX, int tileY) {
        // Street tiles override all solid layers
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
            // On the House layer only actual houses (GID 26–34) block
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
        // Player ရဲ့ ရွေ့လျားမှုကို Update လုပ်မယ်
        player.update(delta);

        // 🎥 ကင်မရာကို Player ရဲ့ ဗဟိုချက်ဆီ လိုက်ခိုင်းခြင်း
        float playerCenterX = player.getX() + (player.getWidth() / 2f);
        float playerCenterY = player.getY() + (player.getHeight() / 2f);

        // Clamp camera so no black borders are visible outside the map
        float halfViewW = (camera.viewportWidth * camera.zoom) / 2f;
        float halfViewH = (camera.viewportHeight * camera.zoom) / 2f;
        float camX = MathUtils.clamp(playerCenterX, halfViewW, mapPixelWidth - halfViewW);
        float camY = MathUtils.clamp(playerCenterY, halfViewH, mapPixelHeight - halfViewH);

        camera.position.set(camX, camY, 0);
        camera.update();

        // နောက်ခံ screen ကို သန့်ရှင်းပေးခြင်း
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1);

        // ၁။ Tiled မြေပုံကို အရင်ဆွဲမယ်
        mapRenderer.setView(camera);
        mapRenderer.render();

        // ၂။ Player ကို မြေပုံအပေါ်ထပ်ကနေ ဆွဲမယ်
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.draw(batch);
        batch.end();

        // ၃။ Draw red outlines on collision tiles
        drawCollisionDebug();

        // ၄။ Draw HUD (HP / EXP bars)
        drawHUD();
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
                    // Skip tiles overridden by a road
                    if (street != null) {
                        TiledMapTileLayer.Cell sc = street.getCell(tx, ty);
                        if (sc != null && sc.getTile() != null) continue;
                    }
                    TiledMapTileLayer.Cell cell = layer.getCell(tx, ty);
                    if (cell != null && cell.getTile() != null) {
                        // On the House layer, only mark actual houses (GID 26–34)
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

        // ── Layout ──
        int margin = 16;
        int padding = 8;
        float barW = 180, barH = 16;
        float labelW = 50;
        float rightPad = 10;
        float panelX = margin;
        float panelW = margin + labelW + padding + barW + padding + rightPad;
        float innerH = barH * 2 + padding * 2 + 20;
        float panelY = screenH - margin - innerH;

        // ── Panel background ──
        hudRenderer.begin(ShapeRenderer.ShapeType.Filled);
        hudRenderer.setColor(0, 0, 0, 0.55f);
        hudRenderer.rect(panelX, panelY, panelW, innerH);
        hudRenderer.end();

        // ── Panel border ──
        hudRenderer.begin(ShapeRenderer.ShapeType.Line);
        hudRenderer.setColor(0.5f, 0.5f, 0.5f, 0.8f);
        hudRenderer.rect(panelX, panelY, panelW, innerH);
        hudRenderer.end();

        float labelX = panelX + margin;
        float barX  = labelX + labelW;
        float contentY = panelY + padding + 10;

        // ── HP bar ──
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

        // HP bar border
        hudRenderer.begin(ShapeRenderer.ShapeType.Line);
        hudRenderer.setColor(0.4f, 0.4f, 0.4f, 1);
        hudRenderer.rect(barX, hpY, barW, barH);
        hudRenderer.end();

        // ── EXP bar ──
        float expY = contentY;
        float expFill = (float)player.getExp() / Math.max(player.getExpToNext(), 1);

        hudRenderer.begin(ShapeRenderer.ShapeType.Filled);
        hudRenderer.setColor(0.15f, 0.15f, 0.15f, 1);
        hudRenderer.rect(barX, expY, barW, barH);

        float expR = 0.2f, expG = 0.3f + 0.5f * expFill, expB = 0.8f;
        hudRenderer.setColor(expR, expG, expB, 1);
        hudRenderer.rect(barX, expY, barW * expFill, barH);
        hudRenderer.end();

        // EXP bar border
        hudRenderer.begin(ShapeRenderer.ShapeType.Line);
        hudRenderer.setColor(0.4f, 0.4f, 0.4f, 1);
        hudRenderer.rect(barX, expY, barW, barH);
        hudRenderer.end();

        // ── Text labels ──
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        hudFont.setColor(0.95f, 0.95f, 0.95f, 1);

        float textY = hpY + (barH + 4);
        hudFont.draw(batch, "HP", labelX, textY);
        hudFont.draw(batch, player.getHealth() + "/" + player.getMaxHealth(), barX + barW - 50, textY);

        textY = expY + (barH + 4);
        hudFont.draw(batch, "EXP", labelX, textY);
        hudFont.draw(batch, player.getExp() + "/" + player.getExpToNext(), barX + barW - 50, textY);

        // Level indicator (below EXP bar)
        hudFont.setColor(1, 0.85f, 0.2f, 1);
        hudFont.draw(batch, "Lv." + player.getLevel(), barX, expY - 2);
        batch.end();
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
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (debugRenderer != null) debugRenderer.dispose();
        if (hudRenderer != null) hudRenderer.dispose();
        if (hudFont != null) hudFont.dispose();
    }
}
