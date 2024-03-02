package steam;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JOptionPane;

public class Steam {
   private RandomAccessFile fileCodes, fileGames, filePlayer;

   public Steam() {
        try {
            File folder = new File("steamFiles");
            if (!folder.exists()) {
                folder.mkdir();
            }          
            fileCodes = new RandomAccessFile("steamFiles/codes.stm", "rw");
            fileGames = new RandomAccessFile("steamFiles/games.stm", "rw");
            filePlayer = new RandomAccessFile("steamFiles/player.stm", "rw");            
            initCodes();        
        } catch(IOException e) {
            JOptionPane.showMessageDialog(null, "Ha ocurrido un error.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void initCodes() throws IOException {
        if (fileCodes.length() == 0) {
            fileCodes.writeInt(1);
            fileCodes.writeInt(1);
            fileCodes.writeInt(1);
        }
    }    
    public int getCode(String tipo) throws IOException {
    int codeActual = -1;
    switch (tipo.toLowerCase()) {
        case "juegos":
            return incrementarCodigo(0);

        case "clientes":
            return incrementarCodigo(4);

        case "descargas":
            return incrementarCodigo(8);

        default:
            JOptionPane.showMessageDialog(null, "Tipo de contador no válido.", "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
    }
}
private int incrementarCodigo(int posicion) throws IOException {
    int codeActual;
    fileCodes.seek(posicion);
    codeActual = fileCodes.readInt();
    fileCodes.seek(posicion);
    fileCodes.writeInt(codeActual + 1);
    return codeActual;
}

    public void addGame(int code, String titulo, char sistemaOperativo, int edadMinima, double precio) throws IOException {

        try {
            fileGames.seek(fileGames.length());
            String tipo = "clientes";
            int codigo = getCode(tipo);
            fileGames.writeInt(codigo);
            fileGames.writeInt(code);
            fileGames.writeUTF(titulo);
            fileGames.writeChar(sistemaOperativo);
            fileGames.writeInt(edadMinima);
            fileGames.writeDouble(precio);
            fileGames.writeInt(0);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
public boolean addPlayer(String username, String password, String nombre, Long fechaNacimiento,String tipoUsuario) throws IOException {
    try {

        if (!validarDatos(username, password ,fechaNacimiento)) {
            return false; 
        }

        filePlayer.seek(filePlayer.length());

        String tipo = "clientes";
        int codigo = getCode(tipo);

        filePlayer.writeInt(codigo);
        filePlayer.writeUTF(username);
        filePlayer.writeUTF(password);
        filePlayer.writeUTF(nombre);
        filePlayer.writeLong(fechaNacimiento);
        filePlayer.writeInt(0);
        filePlayer.writeUTF(tipoUsuario);

        return true; 
    } catch (IOException ex) {
        System.err.println("Error al agregar jugador: " + ex.getMessage());
        ex.printStackTrace();
        return false; // Si hay una excepción, retornar false
    }
}


private boolean validarDatos(String username, String password, Long fechaNacimiento) {
    if (username == null || username.isEmpty() || password == null || password.isEmpty() || fechaNacimiento == null) {
        System.err.println("Error: El nombre de usuario, la contraseña y la fecha de nacimiento son obligatorios.");
        return false;
    }

    return true;
}

private boolean validarUsuario(String username, String password) {
    try {
        filePlayer.seek(0);
        while (filePlayer.getFilePointer() < filePlayer.length()) {
            int codigo = filePlayer.readInt();
            String user = filePlayer.readUTF();
            String pass = filePlayer.readUTF();
            
            if (user.equals(username) && pass.equals(password)) {
                return true;
            }

            filePlayer.readUTF(); 
            filePlayer.readLong(); 
            filePlayer.readInt(); 
            filePlayer.readUTF(); 
        }
    } catch (IOException e) {
        System.err.println("Error al validar usuario: " + e.getMessage());
        e.printStackTrace();
    }
    
    return false;
}
      
    public boolean revisarGame(int gameCode) throws IOException {

        fileGames.seek(0);

        while (fileGames.getFilePointer() < fileGames.length()) {
            int codigo = fileGames.readInt();
            if (codigo == gameCode) {
                return true;
            }
            fileGames.readUTF();
            fileGames.readChar();
            fileGames.readInt();
            fileGames.readDouble();
            fileGames.readInt();
        }

        return false;

    }

    private boolean revisarUsuario(int clientCode) {
        try {
            filePlayer.seek(0);
            while (filePlayer.getFilePointer() < filePlayer.length()) {
                int codigo = filePlayer.readInt();
                if (codigo == clientCode) {
                    return true;
                }

                filePlayer.skipBytes(4 + 8 + 4 + 4);
                filePlayer.readUTF();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean revisargame(int gameCode, char os) {
        try {
            fileGames.seek(0);
            while (fileGames.getFilePointer() < fileGames.length()) {
                int codigo = fileGames.readInt();
                if (codigo == gameCode) {
                    fileGames.skipBytes(6);
                    char sistemaOperativo = fileGames.readChar();
                    return sistemaOperativo == os;
                } else {
                    fileGames.readInt();
                    fileGames.readDouble();
                    fileGames.readInt();

                    int imageLength = fileGames.readInt();
                    fileGames.skipBytes(imageLength);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

 private int calcularEdad(long fechaNacimiento) {
    LocalDate fechaNac = Instant.ofEpochMilli(fechaNacimiento).atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate ahora = LocalDate.now();
    Period periodo = Period.between(fechaNac, ahora);
    return periodo.getYears();
}

private boolean revisarEdadMinima(int clientCode, int gameCode) {
    try {

        int edadMinimaRequerida = getEdadMinimaRequerida(gameCode);
        if (edadMinimaRequerida == -1) {
            return false;
        }
        long fechaNacimiento = getFechaNacimiento(clientCode);
        if (fechaNacimiento == -1) {
            return false;
        }
        int edadCliente = calcularEdad(fechaNacimiento);

        return edadCliente >= edadMinimaRequerida;
    } catch (IOException e) {
        e.printStackTrace();
        return false;
    }
}

    private int getEdadMinimaRequerida(int gameCode) throws IOException {
        fileGames.seek(0);
        while (fileGames.getFilePointer() < fileGames.length()) {
            int codigoJuego = fileGames.readInt();
            if (codigoJuego == gameCode) {
                fileGames.readUTF();
                fileGames.readChar();
                return fileGames.readInt();
            } else {
                fileGames.readDouble();
                fileGames.readInt();
            }
        }
        return -1;
    }

    private long getFechaNacimiento(int clientCode) throws IOException {
        filePlayer.seek(0);
        while (filePlayer.getFilePointer() < filePlayer.length()) {
            int codigoJugador = filePlayer.readInt();
            if (codigoJugador == clientCode) {

                fileGames.readUTF();
                fileGames.readUTF();
                fileGames.readUTF();

                return filePlayer.readLong();
            } else {
                fileGames.readInt();
                fileGames.readUTF();
            }
        }
        return -1;
    }
   public boolean downloadGame(int codigoVG, int codigoCliente, char sistemaOperativo) {
    try {
        // Verificar si el juego existe
        if (!revisarGame(codigoVG)) {
            JOptionPane.showMessageDialog(null, "El juego no existe.");
            return false;
        }

        if (!revisarUsuario(codigoCliente)) {
            JOptionPane.showMessageDialog(null, "El cliente no existe.");
            return false;
        }

        if (!revisargame(codigoVG, sistemaOperativo)) {
            JOptionPane.showMessageDialog(null, "El juego no está disponible para el sistema operativo especificado.");
            return false;
        }

        if (!revisarEdadMinima(codigoCliente, codigoVG)) {
            JOptionPane.showMessageDialog(null, "El cliente no cumple con la edad mínima requerida para descargar el juego.");
            return false;
        }

      
//        actualizarDownloads(codigoVG);

        String downloadFileName = createDownloadFile(codigoVG, codigoCliente);

        JOptionPane.showMessageDialog(null, "Descarga creada con éxito: " + downloadFileName);
        return true;

    } catch (IOException e) {
        e.printStackTrace();
        return false;
    }
}

private String createDownloadFile(int codigoVG, int codigoCliente) throws IOException {
    String downloadFileName = "steam/downloads/" + "download_" + getCode("descargas") + ".stm";
    FileWriter writer = new FileWriter(downloadFileName);

    // Escribir la fecha de descarga
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String formattedDate = now.format(formatter);
    writer.write(formattedDate + "\n");

    // Obtener información del juego
    String gameInfo = getInfoJuego(codigoVG);

    // Obtener nombre del cliente
    String clientName = getNombreCliente(codigoCliente);

    // Escribir información de la descarga
    writer.write("Download #" + codigoVG + "\n");
    writer.write(clientName + " ha descargado " + gameInfo.split("\n")[0] + " a un precio de $ " + gameInfo.split("\n")[4] + ".\n");

    writer.close();
    return downloadFileName;
}
    private String getInfoJuego(int codigoVG) throws IOException {
        fileGames.seek(0);
        while (fileGames.getFilePointer() < fileGames.length()) {
            int codigoJuego = fileGames.readInt();
            if (codigoJuego == codigoVG) {
                String titulo = fileGames.readUTF();
                char sistemaOperativo = fileGames.readChar();
                fileGames.readInt();
                fileGames.readDouble();
                int downloads = fileGames.readInt();
                return titulo + "\n" + sistemaOperativo + "\n" + downloads;
            } 
        }
        return "";
    }

    private String getNombreCliente(int codigoCliente) throws IOException {
        filePlayer.seek(0);
        while (filePlayer.getFilePointer() < filePlayer.length()) {
            int codigo = filePlayer.readInt();
            if (codigo == codigoCliente) {
                return filePlayer.readUTF();
            }
            fileGames.readUTF();
            fileGames.readChar();
            fileGames.readInt();
            fileGames.readDouble();
            fileGames.readInt();    
        }
        return "";
    }
    
    public void updatePriceFor(int gameCode, double newPrice) {
        try {
            fileGames.seek(0);
            while (fileGames.getFilePointer() < fileGames.length()) {
                int codigoJuego = fileGames.readInt();
                if (codigoJuego == gameCode) {
                    fileGames.readUTF();
                    fileGames.readChar();
                    fileGames.readInt();
                    fileGames.writeDouble(newPrice);
                    break;
                } else {
                    fileGames.readInt();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reportForClient(int clientCode, String txtFile) {
        try {
            if (!revisarUsuario(clientCode)) {
                JOptionPane.showMessageDialog(null,"NO SE PUEDE CREAR REPORTE");
                return;
            }
            FileWriter writer = new FileWriter(txtFile, false);
            filePlayer.seek(0);
            while (filePlayer.getFilePointer() < filePlayer.length()) {
                int codigo = filePlayer.readInt();
                if (codigo == clientCode) {
                    writer.write("Datos del Cliente:\n");
                    writer.write("Código: " + codigo + "\n");
                    writer.write("Username: " + filePlayer.readUTF() + "\n");
                    filePlayer.readUTF();
                    filePlayer.readUTF();
                    writer.write("Nacimiento: " + new Date(filePlayer.readLong()) + "\n");
                    writer.write("Contador de Downloads: " + filePlayer.readInt() + "\n");
                    filePlayer.readInt();
                    filePlayer.readUTF();
                    writer.close();
                    JOptionPane.showMessageDialog(null, "REPORTE CREADO");
                    return;
                }
                filePlayer.readUTF();
                filePlayer.readUTF();
                filePlayer.readUTF();
                filePlayer.readLong();
                filePlayer.readInt();
                filePlayer.readUTF();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null,"NO SE PUEDE CREAR REPORTE.");
    }

    public String printGames() {
              
        try {
            fileGames.seek(0);
            while (fileGames.getFilePointer() < fileGames.length()) {
                int codigoJuego = fileGames.readInt();
                String titulo = fileGames.readUTF();
                char sistemaOperativo = fileGames.readChar();
                int edadMinima = fileGames.readInt();
                double precio = fileGames.readDouble();
                int contadorDownloads = fileGames.readInt();
                fileGames.skipBytes(fileGames.readInt());
               JOptionPane.showMessageDialog(null,"Código: "+codigoJuego);
               JOptionPane.showMessageDialog(null,"Titulo: "+titulo);
               JOptionPane.showMessageDialog(null,"sistema operativo: "+sistemaOperativo);
               JOptionPane.showMessageDialog(null,"edad minima: "+edadMinima);
               JOptionPane.showMessageDialog(null,"Costo: "+precio);
               JOptionPane.showMessageDialog(null,"CóntadorDonwloads: "+contadorDownloads);
               
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        return null;
    }       
}
