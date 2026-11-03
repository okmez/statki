import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.scene.input.*;
import javafx.geometry.*;
import javafx.animation.*;
import javafx.util.Duration;

import javafx.concurrent.*;

import java.net.*;
import java.util.*;


// ===================== PLANSZA (moja flota) =====================

class Plansza
 {
  static final int N = 10;
  static final int[] ROZMIARY = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

  int[][] statek = new int[N][N];          // numer statku na polu, -1 = woda
  boolean[][] ostrzelane = new boolean[N][N];
  List<int[][]> statki = new ArrayList<>(); // pola kazdego statku: {{x,y}, {x,y}, ...}

  public Plansza()
   {
    wyczysc();
   }

  void wyczysc()
   {
    for(int[] w : statek) Arrays.fill(w, -1);
    for(boolean[] w : ostrzelane) Arrays.fill(w, false);
    statki.clear();
   }

  // statki nie moga sie stykac, nawet rogami
  boolean mozna_postawic(int x, int y, int dl, boolean poziomo)
   {
    for(int i = 0; i < dl; i++)
     {
      int px = poziomo ? x + i : x;
      int py = poziomo ? y : y + i;
      if(px < 0 || py < 0 || px >= N || py >= N) return false;
      for(int dx = -1; dx <= 1; dx++)
        for(int dy = -1; dy <= 1; dy++)
         {
          int sx = px + dx, sy = py + dy;
          if(sx >= 0 && sy >= 0 && sx < N && sy < N && statek[sx][sy] != -1) return false;
         }
     }
    return true;
   }

  void postaw(int x, int y, int dl, boolean poziomo)
   {
    int[][] pola = new int[dl][];
    for(int i = 0; i < dl; i++)
     {
      int px = poziomo ? x + i : x;
      int py = poziomo ? y : y + i;
      statek[px][py] = statki.size();
      pola[i] = new int[]{px, py};
     }
    statki.add(pola);
   }

  boolean wszystkie_postawione()
   {
    return statki.size() == ROZMIARY.length;
   }

  int nastepny_rozmiar()
   {
    return wszystkie_postawione() ? 0 : ROZMIARY[statki.size()];
   }

  void losuj()
   {
    Random r = new Random();
    while(true)
     {
      wyczysc();
      int proby = 0;
      for(int dl : ROZMIARY)
       {
        boolean ok = false;
        while(!ok && proby < 1000)
         {
          proby++;
          boolean poz = r.nextBoolean();
          int x = r.nextInt(N), y = r.nextInt(N);
          if(mozna_postawic(x, y, dl, poz)) { postaw(x, y, dl, poz); ok = true; }
         }
       }
      if(wszystkie_postawione()) return;
     }
   }

  boolean zatopiony(int nr)
   {
    for(int[] p : statki.get(nr)) if(!ostrzelane[p[0]][p[1]]) return false;
    return true;
   }

  boolean wszystkie_zatopione()
   {
    for(int i = 0; i < statki.size(); i++) if(!zatopiony(i)) return false;
    return true;
   }

  // przyjecie strzalu; dziala tez dla powtorzonego strzalu (zwraca ten sam wynik)
  Komunikat strzal(int x, int y)
   {
    ostrzelane[x][y] = true;
    Komunikat k = new Komunikat(Komunikat.WYNIK);
    k.x = x;
    k.y = y;
    int nr = statek[x][y];
    if(nr == -1) k.wynik = Komunikat.PUDLO;
    else if(!zatopiony(nr)) k.wynik = Komunikat.TRAFIONY;
    else
     {
      k.wynik = Komunikat.ZATOPIONY;
      int[][] pola = statki.get(nr);
      k.statek_x = new int[pola.length];
      k.statek_y = new int[pola.length];
      for(int i = 0; i < pola.length; i++) { k.statek_x[i] = pola[i][0]; k.statek_y[i] = pola[i][1]; }
     }
    k.koniec = wszystkie_zatopione();
    return k;
   }
 }


// ===================== BOT (przeciwnik komputerowy) =====================
// Udaje drugiego gracza: dostaje te same komunikaty, ktore poszlyby przez siec,
// i odpowiada takimi samymi komunikatami. Reszta gry nie wie, ze to nie siec.

class Bot
 {
  static final int N = Plansza.N;
  static final int NIEZNANE = 0, PUDLO = 1, TRAFIONY = 2, ZATOPIONY = 3, PUSTE = 4;

  Plansza plansza = new Plansza();
  int[][] wiedza = new int[N][N];              // co bot wie o planszy gracza
  List<Integer> zostalo = new ArrayList<>();   // dlugosci niezatopionych statkow gracza
  long los;
  boolean w_grze = false;
  Random rnd = new Random();

  void nowa()
   {
    plansza.losuj();
    for(int[] w : wiedza) Arrays.fill(w, NIEZNANE);
    zostalo.clear();
    for(int d : Plansza.ROZMIARY) zostalo.add(d);
    los = rnd.nextLong();
    w_grze = true;
   }

  // odpowiedzi bota na komunikat gracza; strzal bota (gdy to jego ruch) jest zawsze ostatni
  List<Komunikat> odbierz(Komunikat k)
   {
    List<Komunikat> odp = new ArrayList<>();

    switch(k.typ)
     {
      case Komunikat.HALO:
        if(!k.odpowiedz)
         {
          Komunikat h = new Komunikat(Komunikat.HALO);
          h.odpowiedz = true;
          odp.add(h);
         }
        break;

      case Komunikat.GOTOWY:            // bot zawsze ma juz ustawione statki
        boolean pierwszy = !w_grze;
        if(pierwszy) nowa();
        Komunikat g = new Komunikat(Komunikat.GOTOWY);
        g.los = los;
        g.odpowiedz = true;
        odp.add(g);
        if(pierwszy && los > k.los) odp.add(strzal());   // bot wylosowal, ze zaczyna
        break;

      case Komunikat.STRZAL:
        if(!w_grze || k.x < 0 || k.y < 0 || k.x >= N || k.y >= N) break;
        boolean powtorka = plansza.ostrzelane[k.x][k.y];
        Komunikat w = plansza.strzal(k.x, k.y);
        odp.add(w);
        if(w.koniec) w_grze = false;
        else if(!powtorka && w.wynik == Komunikat.PUDLO) odp.add(strzal());   // gracz spudlowal - ruch bota
        break;

      case Komunikat.WYNIK:             // wynik strzalu bota
        if(!w_grze) break;
        zapisz(k);
        if(k.koniec) w_grze = false;
        else if(k.wynik != Komunikat.PUDLO) odp.add(strzal());   // trafil - strzela dalej
        break;
     }
    return odp;
   }

  void zapisz(Komunikat k)
   {
    if(k.wynik == Komunikat.PUDLO) { wiedza[k.x][k.y] = PUDLO; return; }

    wiedza[k.x][k.y] = TRAFIONY;
    for(int dx = -1; dx <= 1; dx += 2)            // na skos od trafienia na pewno jest woda
      for(int dy = -1; dy <= 1; dy += 2)
        if(wolne(k.x + dx, k.y + dy)) wiedza[k.x + dx][k.y + dy] = PUSTE;

    if(k.wynik == Komunikat.ZATOPIONY && k.statek_x != null)
     {
      zostalo.remove(Integer.valueOf(k.statek_x.length));
      for(int i = 0; i < k.statek_x.length; i++)
       {
        int sx = k.statek_x[i], sy = k.statek_y[i];
        wiedza[sx][sy] = ZATOPIONY;
        for(int dx = -1; dx <= 1; dx++)          // wokol zatopionego statku tez woda
          for(int dy = -1; dy <= 1; dy++)
            if(wolne(sx + dx, sy + dy)) wiedza[sx + dx][sy + dy] = PUSTE;
       }
     }
   }

  boolean wolne(int x, int y)
   {
    return x >= 0 && y >= 0 && x < N && y < N && wiedza[x][y] == NIEZNANE;
   }

  Komunikat strzal()
   {
    int[] p = wybierz();
    Komunikat s = new Komunikat(Komunikat.STRZAL);
    s.x = p[0];
    s.y = p[1];
    return s;
   }

  int[] wybierz()
   {
    List<int[]> traf = new ArrayList<>(), kand = new ArrayList<>();
    int[][] kier = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    for(int x = 0; x < N; x++)
      for(int y = 0; y < N; y++)
        if(wiedza[x][y] == TRAFIONY) traf.add(new int[]{x, y});

    // dobijanie: trafiony, ale nie zatopiony statek (statki sie nie stykaja, wiec to zawsze jeden statek)
    if(!traf.isEmpty())
     {
      boolean poziom = traf.size() > 1 && traf.get(0)[1] == traf.get(1)[1];
      boolean pion   = traf.size() > 1 && traf.get(0)[0] == traf.get(1)[0];
      for(int[] t : traf)
        for(int[] d : kier)
         {
          if(poziom && d[1] != 0) continue;     // kilka trafien w linii - strzelamy tylko na jej koncach
          if(pion && d[0] != 0) continue;
          if(wolne(t[0] + d[0], t[1] + d[1])) kand.add(new int[]{t[0] + d[0], t[1] + d[1]});
         }
     }

    if(kand.isEmpty()) kand = najlepsze();
    return kand.get(rnd.nextInt(kand.size()));
   }

  // szukanie: pola, na ktorych miesci sie najwiecej pozostalych statkow
  List<int[]> najlepsze()
   {
    int[][] ile = new int[N][N];
    int max = 0;
    List<int[]> naj = new ArrayList<>();

    for(int dl : zostalo)
      for(int x = 0; x < N; x++)
        for(int y = 0; y < N; y++)
          for(int o = 0; o < 2; o++)
           {
            if(dl == 1 && o == 1) continue;     // jedynka w pionie = to samo co w poziomie
            boolean ok = true;
            for(int i = 0; i < dl && ok; i++) ok = wolne(o == 0 ? x + i : x, o == 0 ? y : y + i);
            if(ok) for(int i = 0; i < dl; i++) ile[o == 0 ? x + i : x][o == 0 ? y : y + i]++;
           }

    for(int x = 0; x < N; x++)
      for(int y = 0; y < N; y++)
        max = Math.max(max, ile[x][y]);

    // troche losowosci: nie tylko najlepsze pole, ale tez prawie najlepsze
    for(int x = 0; x < N; x++)
      for(int y = 0; y < N; y++)
        if(wolne(x, y) && ile[x][y] > 0 && ile[x][y] >= max * 0.85) naj.add(new int[]{x, y});

    if(naj.isEmpty())
      for(int x = 0; x < N; x++)
        for(int y = 0; y < N; y++)
          if(wolne(x, y)) naj.add(new int[]{x, y});
    return naj;
   }
 }


// ===================== ODBIOR Z SIECI (watek w tle) =====================

class Odbior_task extends Task<Void>
 {
  DatagramSocket ds;
  Statki gra;

  public Odbior_task(DatagramSocket ds, Statki gra)
   {
    this.ds = ds;
    this.gra = gra;
   }

  @Override
  protected Void call() throws Exception
   {
    byte[] incomingData = new byte[Komunikat.MAX_ROZMIAR];

    while(!isCancelled())
     {
      DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
      try
       {
        ds.receive(incomingPacket);
       }
      catch(SocketException e)   // gniazdo zamkniete przy wyjsciu
       {
        break;
       }

      try
       {
        int przed = Komunikat.licznik_konstruktorow;
        Komunikat k = Komunikat.z_bajtow(incomingPacket.getData(), incomingPacket.getLength());
        boolean konstruktor = Komunikat.licznik_konstruktorow != przed;
        String od = incomingPacket.getAddress().getHostAddress() + ":" + incomingPacket.getPort();
        int bajty = incomingPacket.getLength();

        Platform.runLater(() -> gra.odebrano(k, od, bajty, konstruktor));
       }
      catch(Exception e)
       {
        System.out.println("Zly pakiet: " + e);
       }
     }
    return null;
   }
 }

class Odbior_service extends Service<Void>
 {
  DatagramSocket ds;
  Statki gra;

  public Odbior_service(DatagramSocket ds, Statki gra)
   {
    this.ds = ds;
    this.gra = gra;
   }

  protected Task<Void> createTask()
   {
    return new Odbior_task(ds, gra);
   }
 }


// ===================== GRA =====================

public class Statki extends Application
 {
  static final int KRATKA = 34;
  static final int N = Plansza.N;
  static final int MOJA_X = 40, OBCA_X = 40 + N * KRATKA + 90, PLANSZA_Y = 60;

  // stan pola na planszy przeciwnika
  static final int NIEZNANE = 0, PUDLO = 1, TRAFIONY = 2, ZATOPIONY = 3, PUSTE = 4;

  enum Stan { NIEPOLACZONY, USTAWIANIE, CZEKAM_NA_PRZECIWNIKA, MOJ_RUCH, RUCH_PRZECIWNIKA, KONIEC }

  Stan stan = Stan.NIEPOLACZONY;

  Plansza moja = new Plansza();
  int[][] obca = new int[N][N];

  boolean poziomo = true;
  int mysz_x = -1, mysz_y = -1;      // pole pod myszka na mojej planszy
  int celx = -1, cely = -1;          // pole pod myszka na planszy przeciwnika

  // siec
  DatagramSocket ds;
  InetAddress ip_przeciwnika;
  int port_przeciwnika;
  Odbior_service odbior;
  boolean przeciwnik_widoczny = false;   // dostalismy od niego jakikolwiek pakiet
  Bot bot;                               // != null = gra z botem zamiast przez siec

  boolean przeciwnik_gotowy = false;
  long moj_los, jego_los;
  int strzal_x = -1, strzal_y = -1;      // strzal czekajacy na WYNIK (-1 = brak)
  int moje_trafienia = 0, jego_trafienia = 0;
  boolean wygrana;

  boolean auto = false;   // tryb testowy: gra sama
  boolean auto_koniec = false;

  // GUI
  Canvas canvas;
  Label status;
  TextArea log;
  TextField pole_moj_port, pole_ip, pole_jego_port;
  Button b_polacz, b_bot, b_losuj, b_wyczysc, b_gotowy, b_nowa;


  public static void main(String[] args)
   {
    launch(args);
   }


  @Override
  public void start(Stage stage)
   {
    // argumenty: [moj_port] [ip_przeciwnika] [port_przeciwnika] [--auto] [--bot]
    List<String> a = new ArrayList<>(getParameters().getRaw());
    auto = a.remove("--auto");
    boolean z_botem = a.remove("--bot");
    String arg_moj   = a.size() > 0 ? a.get(0) : "" + Komunikat.PORT_DOMYSLNY;
    String arg_ip    = a.size() > 1 ? a.get(1) : "10.10.10.165";
    String arg_jego  = a.size() > 2 ? a.get(2) : "" + Komunikat.PORT_DOMYSLNY;

    stage.setTitle("Statki przez siec (UDP)");

    // --- pasek polaczenia ---
    pole_moj_port  = new TextField(arg_moj);   pole_moj_port.setPrefColumnCount(5);
    pole_ip        = new TextField(arg_ip);    pole_ip.setPrefColumnCount(11);
    pole_jego_port = new TextField(arg_jego);  pole_jego_port.setPrefColumnCount(5);
    b_polacz = new Button("Polacz");
    b_polacz.setOnAction(e -> polacz());
    b_bot = new Button("Graj z botem");
    b_bot.setOnAction(e -> graj_z_botem());

    HBox pasek1 = new HBox(8, new Label("Moj port:"), pole_moj_port,
                              new Label("IP przeciwnika:"), pole_ip,
                              new Label("Jego port:"), pole_jego_port, b_polacz,
                              new Label("  albo"), b_bot);
    pasek1.setAlignment(Pos.CENTER_LEFT);

    // --- pasek gry ---
    b_losuj   = new Button("Losuj statki");
    b_wyczysc = new Button("Wyczysc");
    b_gotowy  = new Button("Gotowy!");
    b_nowa    = new Button("Nowa gra");
    b_losuj.setOnAction(e -> { moja.losuj(); odswiez(); });
    b_wyczysc.setOnAction(e -> { moja.wyczysc(); odswiez(); });
    b_gotowy.setOnAction(e -> gotowy());
    b_nowa.setOnAction(e -> nowa_gra());

    HBox pasek2 = new HBox(8, b_losuj, b_wyczysc, b_gotowy, b_nowa,
                           new Label("   Ustawianie: lewy klik = postaw, prawy klik / R = obroc"));
    pasek2.setAlignment(Pos.CENTER_LEFT);

    canvas = new Canvas(OBCA_X + N * KRATKA + 30, PLANSZA_Y + N * KRATKA + 30);
    canvas.setOnMouseMoved(this::mysz);
    canvas.setOnMouseExited(e -> { mysz_x = mysz_y = celx = cely = -1; rysuj(); });
    canvas.setOnMouseClicked(this::klik);

    status = new Label();
    status.setFont(Font.font("System", FontWeight.BOLD, 16));

    log = new TextArea();
    log.setEditable(false);
    log.setPrefRowCount(7);

    VBox root = new VBox(8, pasek1, pasek2, canvas, status, log);
    root.setPadding(new Insets(10));

    Scene scene = new Scene(root);
    scene.setOnKeyPressed(e -> { if(e.getCode() == KeyCode.R) { poziomo = !poziomo; rysuj(); } });

    stage.setScene(scene);
    stage.setOnCloseRequest(e -> zamknij());
    stage.show();

    // co sekunde ponawiamy to, na co nie przyszla odpowiedz (UDP moze zgubic pakiet)
    Timeline ponawianie = new Timeline(new KeyFrame(Duration.seconds(1), e -> ponow()));
    ponawianie.setCycleCount(Animation.INDEFINITE);
    ponawianie.play();

    odswiez();

    if(z_botem && !auto) graj_z_botem();

    if(auto)
     {
      dopisz("TRYB AUTO: gra sama (test)");
      if(z_botem) graj_z_botem();
      else        polacz();
      moja.losuj();
      Timeline t = new Timeline(new KeyFrame(Duration.millis(120), e -> auto_krok()));
      t.setCycleCount(Animation.INDEFINITE);
      t.play();
     }
   }


  // ===================== SIEC =====================

  void polacz()
   {
    try
     {
      int moj_port = Integer.parseInt(pole_moj_port.getText().trim());
      ip_przeciwnika = InetAddress.getByName(pole_ip.getText().trim());
      port_przeciwnika = Integer.parseInt(pole_jego_port.getText().trim());

      ds = new DatagramSocket(moj_port);   // slucha na wszystkich kartach sieciowych
     }
    catch(Exception e)
     {
      dopisz("Blad polaczenia: " + e.getMessage());
      return;
     }

    dopisz("Slucham na porcie " + ds.getLocalPort() + ", moje adresy: " + moje_adresy());
    dopisz("Przeciwnik: " + ip_przeciwnika.getHostAddress() + ":" + port_przeciwnika);

    odbior = new Odbior_service(ds, this);
    odbior.start();

    pole_moj_port.setDisable(true);
    pole_ip.setDisable(true);
    pole_jego_port.setDisable(true);
    b_polacz.setDisable(true);
    b_bot.setDisable(true);

    stan = Stan.USTAWIANIE;
    wyslij(new Komunikat(Komunikat.HALO));
    odswiez();
   }

  // gra z botem na jednym komputerze, bez sieci
  void graj_z_botem()
   {
    bot = new Bot();

    pole_moj_port.setDisable(true);
    pole_ip.setDisable(true);
    pole_jego_port.setDisable(true);
    b_polacz.setDisable(true);
    b_bot.setDisable(true);

    dopisz("=== GRA Z BOTEM (bez sieci) ===");
    stan = Stan.USTAWIANIE;
    wyslij(new Komunikat(Komunikat.HALO));
    odswiez();
   }

  // komunikat idzie do bota tak samo jak przez siec: obiekt -> bajty -> obiekt
  void do_bota(Komunikat k)
   {
    try
     {
      byte[] data = k.do_bajtow();
      dopisz(">> do bota " + k + " (" + data.length + " B)");

      double t = 0.15;
      for(Komunikat o : bot.odbierz(Komunikat.z_bajtow(data, data.length)))
       {
        if(o.typ == Komunikat.STRZAL) t += 0.6 + Math.random() * 0.5;   // bot "mysli" przed strzalem
        byte[] b = o.do_bajtow();
        PauseTransition p = new PauseTransition(Duration.seconds(t));
        p.setOnFinished(e -> od_bota(b));
        p.play();
       }
     }
    catch(Exception e)
     {
      dopisz("Blad bota: " + e);
     }
   }

  void od_bota(byte[] b)
   {
    try
     {
      int przed = Komunikat.licznik_konstruktorow;
      Komunikat k = Komunikat.z_bajtow(b, b.length);
      odebrano(k, "BOT", b.length, Komunikat.licznik_konstruktorow != przed);
     }
    catch(Exception e)
     {
      dopisz("Blad bota: " + e);
     }
   }

  void wyslij(Komunikat k)
   {
    if(bot != null) { do_bota(k); return; }

    try
     {
      byte[] data = k.do_bajtow();
      DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_przeciwnika, port_przeciwnika);
      ds.send(sendPacket);
      dopisz(">> wyslano " + k + " (" + data.length + " B)");
     }
    catch(Exception e)
     {
      dopisz("Blad wysylania: " + e.getMessage());
     }
   }

  // wywolywane w watku JavaFX dla kazdego odebranego pakietu
  void odebrano(Komunikat k, String od, int bajty, boolean konstruktor)
   {
    dopisz("<< odebrano " + k + " od " + od + " (" + bajty + " B), konstruktor przy odbiorze: "
           + (konstruktor ? "TAK" : "NIE") + ", licznik konstruktorow = " + Komunikat.licznik_konstruktorow);

    przeciwnik_widoczny = true;

    switch(k.typ)
     {
      case Komunikat.HALO:
        if(!k.odpowiedz)
         {
          Komunikat odp = new Komunikat(Komunikat.HALO);
          odp.odpowiedz = true;
          wyslij(odp);
         }
        break;

      case Komunikat.GOTOWY:
        odebrano_gotowy(k);
        break;

      case Komunikat.STRZAL:
        odebrano_strzal(k);
        break;

      case Komunikat.WYNIK:
        odebrano_wynik(k);
        break;
     }
    odswiez();
   }

  void ponow()
   {
    if(ds == null) return;
    if(!przeciwnik_widoczny) wyslij(new Komunikat(Komunikat.HALO));
    if(stan == Stan.CZEKAM_NA_PRZECIWNIKA) wyslij_gotowy(false);
    if(stan == Stan.MOJ_RUCH && strzal_x >= 0) wyslij_strzal();
   }


  // ===================== PRZEBIEG GRY =====================

  void gotowy()
   {
    if(stan != Stan.USTAWIANIE || !moja.wszystkie_postawione()) return;
    moj_los = new Random().nextLong();
    if(przeciwnik_gotowy)
     {
      wyslij_gotowy(true);
      zacznij();
     }
    else
     {
      stan = Stan.CZEKAM_NA_PRZECIWNIKA;
      wyslij_gotowy(false);
     }
    odswiez();
   }

  void wyslij_gotowy(boolean odpowiedz)
   {
    Komunikat k = new Komunikat(Komunikat.GOTOWY);
    k.los = moj_los;
    k.odpowiedz = odpowiedz;   // true = "wiem, ze ty tez jestes gotowy"
    wyslij(k);
   }

  void odebrano_gotowy(Komunikat k)
   {
    if(stan == Stan.KONIEC)          // przeciwnik zaczal juz nowa gre
     {
      nowa_gra();
     }

    jego_los = k.los;
    przeciwnik_gotowy = true;

    if(stan == Stan.USTAWIANIE)
     {
      dopisz("Przeciwnik ma juz ustawione statki.");
     }
    else if(stan == Stan.CZEKAM_NA_PRZECIWNIKA)
     {
      if(!k.odpowiedz) wyslij_gotowy(true);
      zacznij();
     }
    else if(!k.odpowiedz)            // jestesmy w grze, a on nie dostal naszego GOTOWY
     {
      wyslij_gotowy(true);
     }
   }

  void zacznij()
   {
    boolean ja_zaczynam = moj_los > jego_los;
    stan = ja_zaczynam ? Stan.MOJ_RUCH : Stan.RUCH_PRZECIWNIKA;
    dopisz("=== START GRY, zaczyna " + (ja_zaczynam ? "TY" : "PRZECIWNIK") + " ===");
   }

  void strzelaj(int x, int y)
   {
    if(stan != Stan.MOJ_RUCH || strzal_x >= 0 || obca[x][y] != NIEZNANE) return;
    strzal_x = x;
    strzal_y = y;
    wyslij_strzal();
    odswiez();
   }

  void wyslij_strzal()
   {
    Komunikat k = new Komunikat(Komunikat.STRZAL);
    k.x = strzal_x;
    k.y = strzal_y;
    wyslij(k);
   }

  void odebrano_strzal(Komunikat k)
   {
    if(k.x < 0 || k.y < 0 || k.x >= N || k.y >= N) return;

    boolean powtorka = moja.ostrzelane[k.x][k.y];   // np. zgubil nasza odpowiedz i strzela jeszcze raz
    if(!powtorka && stan != Stan.RUCH_PRZECIWNIKA) return;

    Komunikat odp = moja.strzal(k.x, k.y);
    wyslij(odp);

    if(powtorka) return;

    if(odp.wynik != Komunikat.PUDLO) jego_trafienia++;

    if(odp.koniec)
     {
      wygrana = false;
      stan = Stan.KONIEC;
      dopisz("=== PRZEGRANA ===");
      System.out.println("KONIEC GRY: PRZEGRANA");
     }
    else if(odp.wynik == Komunikat.PUDLO)
     {
      stan = Stan.MOJ_RUCH;   // pudlo - ruch przechodzi na mnie
     }
   }

  void odebrano_wynik(Komunikat k)
   {
    if(stan != Stan.MOJ_RUCH || k.x != strzal_x || k.y != strzal_y) return;   // stary / powtorzony pakiet
    strzal_x = strzal_y = -1;

    if(k.wynik == Komunikat.PUDLO)
     {
      obca[k.x][k.y] = PUDLO;
      stan = Stan.RUCH_PRZECIWNIKA;
     }
    else
     {
      moje_trafienia++;
      obca[k.x][k.y] = TRAFIONY;
      if(k.wynik == Komunikat.ZATOPIONY && k.statek_x != null)
       {
        for(int i = 0; i < k.statek_x.length; i++)
         {
          int sx = k.statek_x[i], sy = k.statek_y[i];
          obca[sx][sy] = ZATOPIONY;
          for(int dx = -1; dx <= 1; dx++)      // wokol zatopionego statku na pewno jest woda
            for(int dy = -1; dy <= 1; dy++)
             {
              int ox = sx + dx, oy = sy + dy;
              if(ox >= 0 && oy >= 0 && ox < N && oy < N && obca[ox][oy] == NIEZNANE) obca[ox][oy] = PUSTE;
             }
         }
       }
      // trafienie = strzelam dalej
     }

    if(k.koniec)
     {
      wygrana = true;
      stan = Stan.KONIEC;
      dopisz("=== WYGRANA ===");
      System.out.println("KONIEC GRY: WYGRANA");
     }
   }

  void nowa_gra()
   {
    if(stan == Stan.NIEPOLACZONY) return;
    moja.wyczysc();
    for(int[] w : obca) Arrays.fill(w, NIEZNANE);
    przeciwnik_gotowy = false;
    strzal_x = strzal_y = -1;
    moje_trafienia = jego_trafienia = 0;
    stan = Stan.USTAWIANIE;
    if(auto) moja.losuj();
    odswiez();
   }

  void auto_krok()
   {
    if(stan == Stan.USTAWIANIE && moja.wszystkie_postawione()) gotowy();
    if(stan == Stan.MOJ_RUCH && strzal_x < 0)
     {
      List<int[]> wolne = new ArrayList<>();
      for(int x = 0; x < N; x++)
        for(int y = 0; y < N; y++)
          if(obca[x][y] == NIEZNANE) wolne.add(new int[]{x, y});
      int[] p = wolne.get(new Random().nextInt(wolne.size()));
      strzelaj(p[0], p[1]);
     }
    if(stan == Stan.KONIEC && !auto_koniec)
     {
      auto_koniec = true;
      PauseTransition p = new PauseTransition(Duration.seconds(3));
      p.setOnFinished(e -> zamknij());
      p.play();
     }
   }


  // ===================== MYSZ =====================

  int pole_x(double px, int x0) { return (int)Math.floor((px - x0) / KRATKA); }
  int pole_y(double py)         { return (int)Math.floor((py - PLANSZA_Y) / KRATKA); }
  boolean na_planszy(int x, int y) { return x >= 0 && y >= 0 && x < N && y < N; }

  void mysz(MouseEvent e)
   {
    mysz_x = pole_x(e.getX(), MOJA_X);  mysz_y = pole_y(e.getY());
    celx   = pole_x(e.getX(), OBCA_X);  cely   = pole_y(e.getY());
    if(!na_planszy(mysz_x, mysz_y)) mysz_x = mysz_y = -1;
    if(!na_planszy(celx, cely))     celx = cely = -1;
    rysuj();
   }

  void klik(MouseEvent e)
   {
    mysz(e);
    if(e.getButton() == MouseButton.SECONDARY) { poziomo = !poziomo; rysuj(); return; }

    if(stan == Stan.USTAWIANIE && mysz_x >= 0 && !moja.wszystkie_postawione())
     {
      int dl = moja.nastepny_rozmiar();
      if(moja.mozna_postawic(mysz_x, mysz_y, dl, poziomo)) moja.postaw(mysz_x, mysz_y, dl, poziomo);
      odswiez();
     }
    else if(celx >= 0)
     {
      strzelaj(celx, cely);
     }
   }


  // ===================== RYSOWANIE =====================

  void odswiez()
   {
    boolean ust = stan == Stan.USTAWIANIE;
    b_losuj.setDisable(!ust);
    b_wyczysc.setDisable(!ust);
    b_gotowy.setDisable(!ust || !moja.wszystkie_postawione());
    b_nowa.setDisable(stan != Stan.KONIEC);

    String s;
    switch(stan)
     {
      case NIEPOLACZONY:          s = "Wpisz adres przeciwnika i kliknij Polacz albo Graj z botem"; break;
      case USTAWIANIE:            s = moja.wszystkie_postawione() ? "Statki ustawione - kliknij Gotowy!"
                                      : "Ustaw statek o dlugosci " + moja.nastepny_rozmiar() + " (lub Losuj statki)"; break;
      case CZEKAM_NA_PRZECIWNIKA: s = "Czekam, az przeciwnik ustawi statki..."; break;
      case MOJ_RUCH:              s = strzal_x >= 0 ? "Strzal w " + Komunikat.pole(strzal_x, strzal_y) + ", czekam na wynik..."
                                      : "TWOJ RUCH - strzelaj w plansze przeciwnika"; break;
      case RUCH_PRZECIWNIKA:      s = "Ruch przeciwnika..."; break;
      default:                    s = wygrana ? "WYGRALES! :)" : "Przegrales :("; break;
     }
    if(stan != Stan.NIEPOLACZONY)
      s += "     |  przeciwnik: " + (bot != null ? "BOT" : przeciwnik_widoczny ? "polaczony" : "brak odpowiedzi")
         + (przeciwnik_gotowy && stan == Stan.USTAWIANIE ? ", gotowy" : "");
    if(stan.ordinal() >= Stan.MOJ_RUCH.ordinal())
      s += "  |  trafienia: ty " + moje_trafienia + "/20, on " + jego_trafienia + "/20";
    status.setText(s);
    rysuj();
   }

  void rysuj()
   {
    GraphicsContext g = canvas.getGraphicsContext2D();
    g.setFill(Color.rgb(16, 24, 40));
    g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

    g.setFill(Color.WHITE);
    g.setFont(Font.font("System", FontWeight.BOLD, 16));
    g.fillText("TWOJA FLOTA", MOJA_X, 25);
    g.fillText("PRZECIWNIK", OBCA_X, 25);

    siatka(g, MOJA_X);
    siatka(g, OBCA_X);

    // moja plansza
    for(int x = 0; x < N; x++)
      for(int y = 0; y < N; y++)
       {
        double px = MOJA_X + x * KRATKA, py = PLANSZA_Y + y * KRATKA;
        int nr = moja.statek[x][y];
        if(nr != -1)
         {
          g.setFill(moja.zatopiony(nr) ? Color.rgb(90, 30, 30) : Color.rgb(150, 160, 175));
          g.fillRect(px + 2, py + 2, KRATKA - 4, KRATKA - 4);
         }
        if(moja.ostrzelane[x][y])
         {
          if(nr != -1) krzyzyk(g, px, py, Color.rgb(255, 70, 50));
          else         kropka(g, px, py, Color.rgb(200, 220, 255), 4);
         }
       }

    // podglad stawianego statku
    if(stan == Stan.USTAWIANIE && mysz_x >= 0 && !moja.wszystkie_postawione())
     {
      int dl = moja.nastepny_rozmiar();
      boolean ok = moja.mozna_postawic(mysz_x, mysz_y, dl, poziomo);
      g.setFill(ok ? Color.rgb(80, 220, 120, 0.6) : Color.rgb(240, 70, 70, 0.6));
      for(int i = 0; i < dl; i++)
       {
        int px = poziomo ? mysz_x + i : mysz_x;
        int py = poziomo ? mysz_y : mysz_y + i;
        if(na_planszy(px, py)) g.fillRect(MOJA_X + px * KRATKA + 2, PLANSZA_Y + py * KRATKA + 2, KRATKA - 4, KRATKA - 4);
       }
     }

    // plansza przeciwnika
    for(int x = 0; x < N; x++)
      for(int y = 0; y < N; y++)
       {
        double px = OBCA_X + x * KRATKA, py = PLANSZA_Y + y * KRATKA;
        switch(obca[x][y])
         {
          case PUDLO:     kropka(g, px, py, Color.rgb(200, 220, 255), 4); break;
          case PUSTE:     kropka(g, px, py, Color.rgb(90, 110, 140), 3); break;
          case TRAFIONY:  krzyzyk(g, px, py, Color.rgb(255, 150, 40)); break;
          case ZATOPIONY:
            g.setFill(Color.rgb(90, 30, 30));
            g.fillRect(px + 2, py + 2, KRATKA - 4, KRATKA - 4);
            krzyzyk(g, px, py, Color.rgb(255, 70, 50));
            break;
         }
        if(x == strzal_x && y == strzal_y)
         {
          g.setStroke(Color.YELLOW);
          g.setLineWidth(3);
          g.strokeRect(px + 3, py + 3, KRATKA - 6, KRATKA - 6);
         }
       }

    if(stan == Stan.MOJ_RUCH && strzal_x < 0 && celx >= 0 && obca[celx][cely] == NIEZNANE)
     {
      g.setStroke(Color.rgb(80, 220, 120));
      g.setLineWidth(3);
      g.strokeRect(OBCA_X + celx * KRATKA + 2, PLANSZA_Y + cely * KRATKA + 2, KRATKA - 4, KRATKA - 4);
     }
   }

  void siatka(GraphicsContext g, int x0)
   {
    g.setFill(Color.rgb(30, 70, 120));
    g.fillRect(x0, PLANSZA_Y, N * KRATKA, N * KRATKA);
    g.setStroke(Color.rgb(60, 110, 170));
    g.setLineWidth(1);
    for(int i = 0; i <= N; i++)
     {
      g.strokeLine(x0 + i * KRATKA, PLANSZA_Y, x0 + i * KRATKA, PLANSZA_Y + N * KRATKA);
      g.strokeLine(x0, PLANSZA_Y + i * KRATKA, x0 + N * KRATKA, PLANSZA_Y + i * KRATKA);
     }
    g.setFill(Color.rgb(180, 200, 230));
    g.setFont(Font.font("System", 13));
    for(int i = 0; i < N; i++)
     {
      g.fillText("" + (char)('A' + i), x0 + i * KRATKA + KRATKA / 2 - 4, PLANSZA_Y - 6);
      g.fillText("" + (i + 1), x0 - 24, PLANSZA_Y + i * KRATKA + KRATKA / 2 + 5);
     }
   }

  void krzyzyk(GraphicsContext g, double px, double py, Color c)
   {
    g.setStroke(c);
    g.setLineWidth(3);
    g.strokeLine(px + 8, py + 8, px + KRATKA - 8, py + KRATKA - 8);
    g.strokeLine(px + KRATKA - 8, py + 8, px + 8, py + KRATKA - 8);
   }

  void kropka(GraphicsContext g, double px, double py, Color c, double r)
   {
    g.setFill(c);
    g.fillOval(px + KRATKA / 2 - r, py + KRATKA / 2 - r, 2 * r, 2 * r);
   }


  // ===================== RESZTA =====================

  void dopisz(String s)
   {
    System.out.println(s);
    log.appendText(s + "\n");
   }

  static String moje_adresy()
   {
    StringBuilder sb = new StringBuilder();
    try
     {
      for(NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces()))
       {
        if(!ni.isUp() || ni.isLoopback()) continue;
        for(InetAddress addr : Collections.list(ni.getInetAddresses()))
          if(addr instanceof Inet4Address) sb.append(addr.getHostAddress()).append(" ");
       }
     }
    catch(SocketException e) { }
    return sb.toString().trim();
   }

  void zamknij()
   {
    if(odbior != null) odbior.cancel();
    if(ds != null) ds.close();
    Platform.exit();
   }
 }
