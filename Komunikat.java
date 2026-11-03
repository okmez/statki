import java.io.*;


// =====================================================================
//  Komunikat - wlasna klasa komunikacyjna gry w statki.
//
//  Obiekt jest pakowany do tablicy bajtow (ObjectOutputStream ->
//  ByteArrayOutputStream) i wysylany przez UDP do drugiego komputera.
//
//  Po odebraniu tablica bajtow jest zamieniana z powrotem na obiekt
//  (ObjectInputStream). Przy tym KONSTRUKTOR Komunikat NIE JEST
//  URUCHAMIANY - Java odtwarza pola prosto z bajtow (dla klasy
//  Serializable uruchamia sie tylko konstruktor pierwszej
//  nieserializowalnej nadklasy, czyli Object).
//  Widac to po liczniku "licznik_konstruktorow": rosnie tylko przy
//  tworzeniu komunikatow do wyslania, a nie przy odbiorze.
// =====================================================================

public class Komunikat implements Serializable
 {
  private static final long serialVersionUID = 1;

  // ---------- stale komunikacji ----------
  public static final int PORT_DOMYSLNY = 6000;   // port UDP gry
  public static final int MAX_ROZMIAR   = 1024;   // max rozmiar pakietu w bajtach

  // typy komunikatow
  public static final int HALO    = 1;   // "jestem", sprawdzenie polaczenia
  public static final int GOTOWY  = 2;   // statki ustawione, w polu los liczba do losowania kto zaczyna
  public static final int STRZAL  = 3;   // strzal w pole (x, y)
  public static final int WYNIK   = 4;   // odpowiedz na strzal

  // wyniki strzalu
  public static final int PUDLO     = 0;
  public static final int TRAFIONY  = 1;
  public static final int ZATOPIONY = 2;

  // licznik wywolan konstruktora (static - nie jest wysylany)
  public static int licznik_konstruktorow = 0;

  // ---------- dane wysylane ----------
  public int typ;
  public int x, y;              // pole strzalu
  public int wynik;             // PUDLO / TRAFIONY / ZATOPIONY
  public boolean odpowiedz;     // true = to jest odpowiedz (zeby nie odbijac sobie pakietow w nieskonczonosc)
  public boolean koniec;        // true = wszystkie statki zatopione, koniec gry
  public long los;              // do losowania kto zaczyna
  public int[] statek_x;        // pola zatopionego statku (tylko przy ZATOPIONY)
  public int[] statek_y;


  public Komunikat(int typ)
   {
    this.typ = typ;
    licznik_konstruktorow++;
    System.out.println("[Komunikat] konstruktor uruchomiony (" + nazwa_typu(typ) + "), licznik = " + licznik_konstruktorow);
   }


  // obiekt -> tablica bajtow
  public byte[] do_bajtow() throws IOException
   {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(MAX_ROZMIAR);
    ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
    os.flush();
    os.writeObject(this);
    os.flush();
    os.close();
    return byteStream.toByteArray();
   }


  // tablica bajtow -> obiekt (bez uruchamiania konstruktora!)
  public static Komunikat z_bajtow(byte[] data, int dlugosc) throws IOException, ClassNotFoundException
   {
    ByteArrayInputStream in = new ByteArrayInputStream(data, 0, dlugosc);
    ObjectInputStream is = new ObjectInputStream(in);
    Komunikat k = (Komunikat) is.readObject();
    is.close();
    return k;
   }


  public static String nazwa_typu(int typ)
   {
    switch(typ)
     {
      case HALO:   return "HALO";
      case GOTOWY: return "GOTOWY";
      case STRZAL: return "STRZAL";
      case WYNIK:  return "WYNIK";
      default:     return "?" + typ;
     }
   }


  public static String pole(int x, int y)
   {
    return "" + (char)('A' + x) + (y + 1);
   }


  @Override
  public String toString()
   {
    String s = nazwa_typu(typ);
    if(typ == STRZAL) s += " " + pole(x, y);
    if(typ == WYNIK)
     {
      s += " " + pole(x, y) + " = " + (wynik == PUDLO ? "pudlo" : wynik == TRAFIONY ? "trafiony" : "zatopiony");
      if(koniec) s += " (koniec gry)";
     }
    if(odpowiedz) s += " [odp]";
    return s;
   }
 }
