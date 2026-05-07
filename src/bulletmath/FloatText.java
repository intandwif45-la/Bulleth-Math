package bulletmath;

import java.awt.*;

/** Teks mengambang yang muncul saat skor berubah. */
public class FloatText {
    private float x, y;
    private String text;
    private Color  color;
    private float  life = 1.0f;
    public  boolean alive = true;

    public FloatText(String text, int x, int y, Color col) {
        this.text  = text;
        this.x     = x;
        this.y     = y;
        this.color = col;
    }

    public void update() {
        y -= 1.5f;
        life -= 0.025f;
        if (life <= 0) alive = false;
    }

    public void draw(Graphics2D g) {
        if (!alive) return;
        int a = (int)(life * 255);
        g.setFont(new Font("Arial Black", Font.BOLD, 22));
        g.setColor(new Color(0,0,0,Math.max(0,a-80)));
        FontMetrics fm = g.getFontMetrics();
        int tx = (int)x - fm.stringWidth(text)/2;
        g.drawString(text, tx+2, (int)y+2);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0,a)));
        g.drawString(text, tx, (int)y);
    }
}
