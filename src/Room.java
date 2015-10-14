import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Point;

public class Room {
    public List<Point> vertices = new ArrayList<Point>();
    public String name;
    public int left, right, top, bottom;

    public Room(String name, List<Point> vertices) {
        this.name = name;
        this.vertices.addAll(vertices);
        boolean f = true;
        for (Point vertice : this.vertices) {
            if (f || vertice.x < left)
                left = vertice.x;
            if (f || vertice.x > right)
                right = vertice.x;
            if (f || vertice.y < top)
                top = vertice.y;
            if (f || vertice.y > bottom)
                bottom = vertice.y;
            f = false;
        }
    }

    public String toString() {
        return name + " (" + vertices.size() + ")";
    }
}
