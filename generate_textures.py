#!/usr/bin/env python3
"""
DLIAssets Texture Generator
Creates placeholder textures for items, blocks, and entities.
Requires: pip install pillow
"""

from PIL import Image, ImageDraw, ImageFont
import os
import sys

# Colors (R, G, B, A)
COLORS = {
    "ruby": (255, 0, 0, 255),
    "ruby_dark": (170, 0, 0, 255),
    "ruby_light": (255, 100, 100, 255),
    "gold": (255, 215, 0, 255),
    "silver": (192, 192, 192, 255),
    "diamond": (0, 200, 255, 255),
    "emerald": (0, 255, 100, 255),
    "obsidian": (20, 20, 40, 255),
    "magenta": (255, 0, 255, 255),
    "checker_a": (255, 0, 255, 255),
    "checker_b": (0, 0, 0, 255),
    "transparent": (0, 0, 0, 0),
}

def create_dir(path):
    os.makedirs(path, exist_ok=True)

def draw_checkerboard(img, size=16, check_size=4):
    """Draw magenta/black checkerboard (missing texture pattern)"""
    draw = ImageDraw.Draw(img)
    for y in range(0, size, check_size):
        for x in range(0, size, check_size):
            color = COLORS["checker_a"] if ((x//check_size + y//check_size) % 2 == 0) else COLORS["checker_b"]
            draw.rectangle([x, y, x+check_size, y+check_size], fill=color)

def draw_gem(img, color_main, color_dark, color_light, size=16):
    """Draw a simple gem shape"""
    draw = ImageDraw.Draw(img)
    cx, cy = size//2, size//2
    # Diamond shape
    points = [
        (cx, 2),      # top
        (size-2, cy), # right
        (cx, size-2), # bottom
        (2, cy),      # left
    ]
    draw.polygon(points, fill=color_main, outline=color_dark)
    # Highlight
    highlight = [(cx, 4), (cx+4, cy), (cx, cy), (cx-4, cy)]
    draw.polygon(highlight, fill=color_light)
    # Inner sparkle
    draw.ellipse([cx-2, cy-2, cx+2, cy+2], fill=(255,255,255,180))

def draw_sword(img, blade_color, hilt_color, size=16):
    """Draw a simple sword"""
    draw = ImageDraw.Draw(img)
    cx = size//2
    # Blade
    draw.rectangle([cx-1, 1, cx+1, size-5], fill=blade_color, outline=(0,0,0,200))
    # Guard
    draw.rectangle([cx-4, size-5, cx+4, size-4], fill=hilt_color)
    # Handle
    draw.rectangle([cx-1, size-4, cx+1, size-1], fill=(100, 60, 20, 255))
    # Pommel
    draw.ellipse([cx-2, size-2, cx+2, size+2], fill=hilt_color)

def draw_block(img, color_main, color_dark, color_light, size=16):
    """Draw a cube-like block (isometric-ish)"""
    draw = ImageDraw.Draw(img)
    # Top face
    draw.polygon([
        (size//2, 0),
        (size, size//4),
        (size//2, size//2),
        (0, size//4)
    ], fill=color_light, outline=color_dark)
    # Right face
    draw.polygon([
        (size, size//4),
        (size, size),
        (size//2, size*3//4),
        (size//2, size//2)
    ], fill=color_main, outline=color_dark)
    # Left face
    draw.polygon([
        (0, size//4),
        (size//2, size//2),
        (size//2, size*3//4),
        (0, size)
    ], fill=color_dark, outline=color_dark)

def draw_ore(img, base_color, ore_color, size=16):
    """Draw stone with ore speckles"""
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, size, size], fill=base_color)
    # Random ore speckles
    import random
    random.seed(42)
    for _ in range(30):
        x = random.randint(1, size-2)
        y = random.randint(1, size-2)
        s = random.randint(1, 3)
        draw.rectangle([x, y, x+s, y+s], fill=ore_color)

def draw_entity_placeholder(img, color, size=64):
    """Draw a simple entity placeholder (front-facing)"""
    draw = ImageDraw.Draw(img)
    cx, cy = size//2, size//2
    # Body
    draw.rectangle([cx-12, cy, cx+12, cy+24], fill=color)
    # Head
    draw.ellipse([cx-10, cy-16, cx+10, cy+4], fill=color)
    # Arms
    draw.rectangle([cx-20, cy, cx-12, cy+20], fill=color)
    draw.rectangle([cx+12, cy, cx+20, cy+20], fill=color)
    # Legs
    draw.rectangle([cx-8, cy+24, cx-2, cy+32], fill=color)
    draw.rectangle([cx+2, cy+24, cx+8, cy+32], fill=color)
    # Eyes
    draw.ellipse([cx-6, cy-10, cx-3, cy-7], fill=(255,255,255,255))
    draw.ellipse([cx+3, cy-10, cx+6, cy-7], fill=(255,255,255,255))

def save_texture(path, generator_func, *args, size=16):
    """Create and save a texture"""
    img = Image.new("RGBA", (size, size), COLORS["transparent"])
    generator_func(img, *args, size=size)
    img.save(path)
    print(f"Created: {path}")

def main():
    base = "textures"
    
    # Create directories
    dirs = [
        "items", "blocks", "entity", "gui", "font", "misc"
    ]
    for d in dirs:
        create_dir(os.path.join(base, d))
    
    print("Generating DLIAssets placeholder textures...")
    
    # ===== ITEMS =====
    save_texture(f"{base}/items/ruby.png", draw_gem, 
                 COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"])
    
    save_texture(f"{base}/items/sapphire.png", draw_gem,
                 (0, 100, 255, 255), (0, 50, 170, 255), (100, 200, 255, 255))
    
    save_texture(f"{base}/items/ruby_sword.png", draw_sword,
                 COLORS["ruby"], COLORS["gold"])
    
    save_texture(f"{base}/items/ruby_pickaxe.png", draw_sword,  # Reuse sword shape
                 COLORS["ruby"], COLORS["silver"])
    
    save_texture(f"{base}/items/ruby_axe.png", draw_sword,
                 COLORS["ruby"], COLORS["silver"])
    
    save_texture(f"{base}/items/ruby_helmet.png", draw_block,
                 COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"])
    
    save_texture(f"{base}/items/ruby_chestplate.png", draw_block,
                 COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"])
    
    save_texture(f"{base}/items/ruby_leggings.png", draw_block,
                 COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"])
    
    save_texture(f"{base}/items/ruby_boots.png", draw_block,
                 COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"])
    
    save_texture(f"{base}/items/ruby_apple.png", draw_gem,
                 (255, 50, 50, 255), (180, 0, 0, 255), (255, 150, 150, 255))
    # Add apple shape overlay
    img = Image.open(f"{base}/items/ruby_apple.png")
    draw = ImageDraw.Draw(img)
    draw.ellipse([3, 3, 13, 13], fill=(200, 0, 0, 255), outline=(150, 0, 0, 255))
    draw.ellipse([5, 5, 9, 9], fill=(255, 100, 100, 200))
    img.save(f"{base}/items/ruby_apple.png")
    
    save_texture(f"{base}/items/guide_book.png", draw_block,
                 (100, 50, 20, 255), (80, 40, 15, 255), (140, 80, 40, 255))
    # Add bookmark
    img = Image.open(f"{base}/items/guide_book.png")
    draw = ImageDraw.Draw(img)
    draw.rectangle([12, 2, 14, 10], fill=(255, 0, 0, 255))
    img.save(f"{base}/items/guide_book.png")
    
    # ===== BLOCKS =====
    save_texture(f"{base}/blocks/ruby_block.png", draw_block,
                 COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"])
    
    save_texture(f"{base}/blocks/ruby_ore.png", draw_ore,
                 (100, 100, 100, 255), COLORS["ruby"])
    
    save_texture(f"{base}/blocks/deepslate_ruby_ore.png", draw_ore,
                 (60, 60, 60, 255), COLORS["ruby"])
    
    save_texture(f"{base}/blocks/ruby_lamp_on.png", draw_block,
                 (255, 255, 150, 255), (255, 200, 0, 255), (255, 255, 255, 255))
    
    save_texture(f"{base}/blocks/ruby_lamp_off.png", draw_block,
                 COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"])
    
    save_texture(f"{base}/blocks/ruby_bricks.png", draw_block,
                 COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"])
    # Add brick lines
    img = Image.open(f"{base}/blocks/ruby_bricks.png")
    draw = ImageDraw.Draw(img)
    for y in range(0, 16, 4):
        draw.line([0, y, 16, y], fill=COLORS["ruby_dark"], width=1)
    for x in range(0, 16, 8):
        offset = 0 if (x//8) % 2 == 0 else 2
        for y in range(offset, 16, 4):
            draw.line([x, y, x, y+4], fill=COLORS["ruby_dark"], width=1)
    img.save(f"{base}/blocks/ruby_bricks.png")
    
    # Crop stages (0-7)
    for stage in range(8):
        img = Image.new("RGBA", (16, 16), COLORS["transparent"])
        draw = ImageDraw.Draw(img)
        # Ground
        draw.rectangle([0, 12, 16, 16], fill=(80, 60, 40, 255))
        # Plant growth
        height = 2 + stage * 1.5
        color = tuple(int(c * (0.5 + stage * 0.06)) for c in COLORS["ruby"][:3]) + (255,)
        draw.rectangle([7, 12-int(height), 9, 12], fill=color)
        if stage >= 5:
            # Buds
            draw.ellipse([5, 10-int(height), 8, 13-int(height)], fill=COLORS["ruby"])
            draw.ellipse([8, 10-int(height), 11, 13-int(height)], fill=COLORS["ruby"])
        if stage == 7:
            # Ripe gem
            draw_gem(img, COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"])
        img.save(f"{base}/blocks/ruby_crop_stage{stage}.png")
    
    # ===== ENTITIES =====
    save_texture(f"{base}/entity/ruby_golem.png", draw_entity_placeholder, COLORS["ruby"], size=64)
    save_texture(f"{base}/entity/ruby_spirit.png", draw_entity_placeholder, (200, 100, 255, 255), size=64)
    
    # ===== GUI / MISC =====
    # Filler glass pane
    img = Image.new("RGBA", (16, 16), COLORS["transparent"])
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 16, 16], fill=(80, 80, 80, 180), outline=(50, 50, 50, 255))
    draw.line([0, 0, 16, 16], fill=(50, 50, 50, 255))
    draw.line([16, 0, 0, 16], fill=(50, 50, 50, 255))
    img.save(f"{base}/gui/filler.png")
    
    # Pack icon
    img = Image.new("RGBA", (128, 128), COLORS["transparent"])
    draw_gem(img, COLORS["ruby"], COLORS["ruby_dark"], COLORS["ruby_light"], size=128)
    img.save(f"{base}/pack.png")
    
    print("\n✅ All textures generated!")
    print(f"Place the '{base}' folder in your DLIAssets plugin directory:")
    print(f"  plugins/DLIAssets/textures/")
    print("\nThen run /dliassets reload to regenerate the resource pack.")

if __name__ == "__main__":
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        print("Pillow not installed. Run: pip install pillow")
        sys.exit(1)
    main()