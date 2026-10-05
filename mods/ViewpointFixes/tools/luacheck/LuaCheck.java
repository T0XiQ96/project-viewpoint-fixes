import java.io.*;
import java.nio.file.*;
import se.krka.kahlua.luaj.compiler.LuaCompiler;

/** Syntaxpruefung von Lua-Dateien mit dem Kahlua-Compiler des Spiels. Aufruf: java -cp projectzomboid.jar;. LuaCheck datei... */
public class LuaCheck {
    public static void main(String[] args) throws Exception {
        int bad = 0;
        for (String a : args) {
            try (Reader r = Files.newBufferedReader(Path.of(a))) {
                LuaCompiler.loadis(r, Path.of(a).getFileName().toString(), null);
                System.out.println("OK   " + a);
            } catch (Throwable t) {
                bad++;
                System.out.println("FEHLER " + a + ": " + t.getMessage());
            }
        }
        System.exit(bad == 0 ? 0 : 1);
    }
}
