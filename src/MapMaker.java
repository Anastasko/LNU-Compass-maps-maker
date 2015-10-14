import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MapMaker {
    private static float scale = 1;

    private Display display;
    protected Shell shell;
    private Image image, originImage;
    private GC graphics;
    ScrolledComposite scrolledComposite;
    List list;
    Label imageLabel = null;

    ArrayList<Room> roomList = new ArrayList<Room>();
    ArrayList<Point> currentArea = new ArrayList<Point>();

    private static final String[] FILTER_NAMES = { "Image (*.png)", "Image (*.jpeg)", "Bitmap (*.bmp)",
            "All Files (*.*)" };
    private static final String[] FILTER_EXTS = { "*.png", "*.jpeg", "*.bmp", "*.*" };

    private static final String[] FILTER_NAMES_SAVE = { "JSON format (*.json)", "All Files (*.*)" };
    private static final String[] FILTER_EXTS_SAVE = { "*.json", "*.*" };

    /**
     * Launch the application.
     *
     * @param args
     * 
     */
    public static double dist(Point a, Point b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
    }

    public void addRoom(Room room) {
        roomList.add(room);
        list.add(roomList.size() + ". " + room);
    }

    public static void main(String[] args) {
        try {
            MapMaker window = new MapMaker();
            window.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Open the window.
     */
    public void open() {
        display = Display.getDefault();
        createContents();
        shell.open();
        shell.layout();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
        if (image != null)
            image.dispose();
    }

    void scaleImage() {
        if (originImage == null)
            return;

        System.out.println("scale");
        final int width = originImage.getBounds().width;
        final int height = originImage.getBounds().height;

        if (image != null)
            image.dispose();
        image = new Image(Display.getDefault(),
                originImage.getImageData().scaledTo((int) (width * scale), (int) (height * scale)));
        imageLabel.setImage(image);
        imageLabel.setSize(imageLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        graphics = new GC(image);
        drawList();
        drawArea(currentArea, false);
        if (list.getSelectionIndex() >= 0)
            showOnMap(list.getSelectionIndex(), false);
    }

    private void showOnMap(int index, boolean doscale) {
        if (doscale)
            scaleImage();
        Room curRoom = roomList.get(index);
        drawArea(curRoom.vertices, true);
    }

    void drawArea(java.util.List<Point> vertices, boolean cycle) {
        try {
            for (int i = 0; i < vertices.size(); ++i) {
                System.out.println(i + " redraw");
                Point p = (Point) vertices.get(i);
                int r = (int) (scale * 4);

                graphics.setBackground(new Color(display, 0, 200, 0));
                graphics.fillOval((int) (p.x * scale - r / 2), (int) (p.y * scale - r / 2), r, r);
                graphics.setLineWidth(2);

                graphics.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
                graphics.drawOval((int) (p.x * scale - r / 2), (int) (p.y * scale - r / 2), r, r);

                if (cycle || i > 0) {
                    graphics.setForeground(display.getSystemColor(SWT.COLOR_RED));
                    Point prev = vertices.get((i - 1 + vertices.size()) % vertices.size());
                    graphics.drawLine((int) (prev.x * scale), (int) (prev.y * scale), (int) (p.x * scale),
                            (int) (p.y * scale));
                }
            }
            imageLabel.setImage(image);
        } catch (Exception exc) {

        }
    }

    void drawList() {
        for (Room room : roomList) {
            int ind = 0;
            int[] pointArray = new int[room.vertices.size() * 2];
            for (Point point : room.vertices) {
                pointArray[ind++] = ((int) (point.x * scale));
                pointArray[ind++] = ((int) (point.y * scale));
            }
            graphics.setAlpha(100);
            graphics.setBackground(new Color(display, 0, 0, 200));
            graphics.fillPolygon(pointArray);
            graphics.setAlpha(255);
        }
        imageLabel.setImage(image);
    }

    /**
     * Create contents of the window.
     */
    protected void createContents() {
        shell = new Shell(display);
        display.addFilter(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event e) {
                int index = list.getSelectionIndex();
                if (e.keyCode == KeyEvent.VK_DELETE && index >= 0) {
                    System.out.println("delete: " + index);
                    roomList.remove(index);
                    list.remove(index);
                    scaleImage();
                }
            }
        });
        shell.setText("Java SWT Application - Map Maker");
        shell.setSize(800, 600);
        shell.setLayout(new FormLayout());

        list = new List(shell, SWT.BORDER | SWT.V_SCROLL);
        list.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = list.getSelectionIndex();
                System.out.println(index + "selected!");
                if (index >= 0) {
                    showOnMap(index, true);
                    Room curRoom = roomList.get(index);
                    // github
                    scrolledComposite.setOrigin(
                            (int) ((curRoom.right + curRoom.left) * scale / 2)
                                    - scrolledComposite.getBounds().width / 2,
                            (int) ((curRoom.bottom + curRoom.top) * scale / 2)
                                    - scrolledComposite.getBounds().height / 2);
                }
            }
        });

        scrolledComposite = new ScrolledComposite(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        FormData fd_scrolledComposite = new FormData();
        fd_scrolledComposite.top = new FormAttachment(0);
        fd_scrolledComposite.bottom = new FormAttachment(100);
        fd_scrolledComposite.right = new FormAttachment(100);
        fd_scrolledComposite.left = new FormAttachment(0, 210);
        scrolledComposite.setLayoutData(fd_scrolledComposite);

        imageLabel = new Label(scrolledComposite, SWT.NONE);
        imageLabel.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_CROSS));
        scrolledComposite.setContent(imageLabel);

        imageLabel.setLayoutData(new FormData());

        // final Node x = new Node(shell, shell, SWT.NONE, display, new
        // Point(10, 300));

        imageLabel.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseDown(MouseEvent e) {
                if (e.button == 1) {
                    if (imageLabel.getCursor().equals(new Cursor(display, SWT.CURSOR_HAND))) {
                        imageLabel.setCursor(new Cursor(display, SWT.CURSOR_CROSS));

                        InputDialog dialog = new InputDialog(shell);
                        String msg = dialog.open();
                        if (msg == null)
                            return;

                        graphics.setForeground(display.getSystemColor(SWT.COLOR_RED));
                        Point prev = currentArea.get(currentArea.size() - 1);
                        Point th = currentArea.get(0);
                        graphics.drawLine((int) (prev.x * scale), (int) (prev.y * scale), (int) (th.x * scale),
                                (int) (th.y * scale));
                        imageLabel.setImage(image);
                        addRoom(new Room(msg, currentArea));
                        currentArea.clear();
                        list.select(list.getItems().length - 1);
                        scaleImage();
                        return;
                    }

                    int x = (int) (e.x / scale + 0.5);
                    int y = (int) (e.y / scale + 0.5);
                    int r = (int) (scale * 4);

                    System.out.println("mouse down " + e.x + " " + e.y);

                    graphics.setBackground(new Color(display, 0, 200, 0));
                    graphics.fillOval(e.x - r / 2, e.y - r / 2, r, r);
                    graphics.setLineWidth(2);

                    graphics.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
                    graphics.drawOval(e.x - r / 2, e.y - r / 2, r, r);

                    if (currentArea.size() > 0) {
                        graphics.setForeground(display.getSystemColor(SWT.COLOR_RED));
                        Point prev = currentArea.get(currentArea.size() - 1);
                        graphics.drawLine((int) (prev.x * scale), (int) (prev.y * scale), e.x, e.y);
                    }

                    imageLabel.setImage(image);

                    currentArea.add(new Point(x, y));
                    System.out.println("add to list");
                }
                if (e.button == 3) {
                    if (currentArea.size() > 0) {
                        currentArea.remove(currentArea.size() - 1);
                        scaleImage();
                    }
                }
            }

            @Override
            public void mouseDoubleClick(MouseEvent arg0) {
                // TODO Auto-generated method stub

            }
        });

        imageLabel.addMouseMoveListener(new MouseMoveListener() {

            @Override
            public void mouseMove(MouseEvent e) {
                if (currentArea.size() <= 2)
                    return;
                Point f = new Point(currentArea.get(0).x, currentArea.get(0).y);

                f.x *= scale;
                f.y *= scale;

                if (dist(f, new Point(e.x, e.y)) <= scale * 2) {
                    imageLabel.setCursor(new Cursor(display, SWT.CURSOR_HAND));
                } else {
                    imageLabel.setCursor(new Cursor(display, SWT.CURSOR_CROSS));
                }

            }
        });

        FormData fd_list = new FormData();
        fd_list.top = new FormAttachment(scrolledComposite, 0, SWT.TOP);
        fd_list.right = new FormAttachment(scrolledComposite, -6);
        fd_list.left = new FormAttachment(0);
        list.setLayoutData(fd_list);

        Menu menu = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menu);

        MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
        mntmFile.setText("File");

        Menu menu_1 = new Menu(mntmFile);
        mntmFile.setMenu(menu_1);

        MenuItem mntmOpenImage = new MenuItem(menu_1, SWT.NONE);
        mntmOpenImage.setToolTipText("Open map to set areas on it");
        mntmOpenImage.setText("Open image ...");

        MenuItem mntmOpenInfo = new MenuItem(menu_1, SWT.NONE);
        mntmOpenInfo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
                String filePath = fileDialog.open();
                if (filePath == null)
                    return;
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
                    roomList.clear();
                    list.removeAll();
                    StringBuilder stringBuilder = new StringBuilder();
                    String line = null;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line + "\n");
                    }
                    bufferedReader.close();
                    System.out.println(stringBuilder.toString());
                    parseJSON(stringBuilder.toString());
                    scaleImage();
                } catch (FileNotFoundException e1) {
                    MessageDialog.openError(shell, "Error", "File not found.");
                    e1.printStackTrace();
                    return;
                } catch (Exception e1) {
                    MessageDialog.openError(shell, "Error", "Can't read file.");
                    e1.printStackTrace();
                }
            }

            private void parseJSON(String jsonText) {
                JSONParser parser = new JSONParser();
                try {
                    JSONArray json = (JSONArray) parser.parse(jsonText);
                    System.out.println("parsed!");
                    System.out.println("str=" + json.toString());
                    for (Object obj : json) {
                        JSONObject area = (JSONObject) obj;
                        String name = (String) area.get("name");
                        System.out.println("name: " + name);
                        JSONArray coords = (JSONArray) area.get("coords");
                        ArrayList<Point> points = new ArrayList<Point>();
                        for (Object coord : coords) {
                            JSONObject jpoint = (JSONObject) coord;
                            String x = jpoint.get("x") + "";
                            String y = jpoint.get("y") + "";
                            System.out.println("(" + x + "," + y + ")");
                            points.add(new Point(Integer.parseInt(x), Integer.parseInt(y)));
                        }
                        addRoom(new Room(name, points));
                    }
                } catch (ParseException pex) {
                    System.out.println("position: " + pex.getPosition());
                    System.out.println(pex);
                    MessageDialog.openError(shell, "Open Error", "Error: " + pex);
                    return;
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    MessageDialog.openError(shell, "Open Error", "Error: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }
        });
        mntmOpenInfo.setText("Open info ...");

        new MenuItem(menu_1, SWT.SEPARATOR);

        MenuItem mntmSaveInfo = new MenuItem(menu_1, SWT.NONE);
        mntmSaveInfo.addSelectionListener(new SelectionAdapter() {
            @SuppressWarnings("unchecked")
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(shell, SWT.SAVE);
                dialog.setFilterExtensions(FILTER_EXTS_SAVE);
                dialog.setFilterNames(FILTER_NAMES_SAVE);
                String fileName = dialog.open();
                if (fileName == null)
                    return;
                JSONArray json = new JSONArray();
                for (Room room : roomList) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", room.name);
                    JSONArray arr = new JSONArray();
                    for (int i = 0; i < room.vertices.size(); ++i) {
                        JSONObject coord = new JSONObject();
                        coord.put("x", room.vertices.get(i).x);
                        coord.put("y", room.vertices.get(i).y);
                        arr.add(coord);
                    }
                    obj.put("coords", arr);
                    json.add(obj);
                }
                System.out.println(json.toString());
                PrintWriter out = null;
                try {
                    out = new PrintWriter(fileName);
                    out.println(json.toString());
                    out.close();
                } catch (FileNotFoundException e1) {
                    MessageDialog.openError(shell, "Can't save", "File not fount");
                    e1.printStackTrace();
                }
            }
        });
        mntmSaveInfo.setText("Save info ...");

        MenuItem mntmZoom = new MenuItem(menu, SWT.CASCADE);
        mntmZoom.setText("Zoom");

        Menu menu_2 = new Menu(mntmZoom);
        mntmZoom.setMenu(menu_2);

        MenuItem menuItem = new MenuItem(menu_2, SWT.RADIO);
        menuItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                scale = 0.5f;
                scaleImage();
            }
        });
        menuItem.setText("50%");

        MenuItem menuItem_1 = new MenuItem(menu_2, SWT.RADIO);
        menuItem_1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                scale = 1f;
                scaleImage();
            }
        });
        menuItem_1.setSelection(true);
        menuItem_1.setText("100%");

        MenuItem menuItem_2 = new MenuItem(menu_2, SWT.RADIO);
        menuItem_2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                scale = 1.5f;
                System.out.println("1.5");
                scaleImage();
            }
        });
        menuItem_2.setText("150%");

        MenuItem menuItem_3 = new MenuItem(menu_2, SWT.RADIO);
        menuItem_3.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                scale = 2f;
                System.out.println("2");
                scaleImage();
            }
        });
        menuItem_3.setText("200%");

        MenuItem menuItem_4 = new MenuItem(menu_2, SWT.RADIO);
        menuItem_4.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                scale = 3f;
                scaleImage();
            }
        });
        menuItem_4.setText("300%");

        MenuItem menuItem_5 = new MenuItem(menu_2, SWT.RADIO);
        menuItem_5.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                scale = 5f;
                scaleImage();
            }
        });
        menuItem_5.setText("500%");
        fd_list.bottom = new FormAttachment(100);

        mntmOpenImage.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(shell, SWT.OPEN);
                dialog.setFilterNames(FILTER_NAMES);
                dialog.setFilterExtensions(FILTER_EXTS);
                dialog.setFileName("map.bmp");
                String fileName = dialog.open();
                if (fileName == null)
                    return;
                try {
                    originImage = new Image(display, fileName);
                    scaleImage();
                } catch (Exception exc) {
                    MessageDialog.openError(shell, "bad format", "Open an image, please!");
                    return;
                }
            }
        });

    }
}
