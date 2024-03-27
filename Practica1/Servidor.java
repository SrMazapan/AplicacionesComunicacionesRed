import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class Servidor {
    private static String rutaDirectorioRemoto = "C:\\Users\\jemma\\Documents\\remoto";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            while (true) {
                Socket socket = serverSocket.accept();
                handleClient(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        ) {
            String opcion;
            while (true) {
                opcion = in.readUTF();
                switch (opcion) {
                    case "1":
                    System.out.println("..");
                    break;
                    case "2":
                    System.out.println("..");
                    break;
                    case "3":
                    System.out.println("..");
                    break;
                    case "4":
                    System.out.println("..");
                    break;
                    case "5":
                        mostrarContenidoCarpetaRemota(out, rutaDirectorioRemoto, "");
                        out.writeUTF("FIN");
                        break;
                    case "6":
                        String nombreCarpeta = in.readUTF();
                        crearCarpetaRemota(out, nombreCarpeta);
                        break;
                    case "7":
                        String nombreArchivo = in.readUTF();
                        borrarRemota(out, nombreArchivo);
                        break;
                    case "8":
                        String nuevaRuta = in.readUTF();
                        cambiarRutaDirectorioRemoto(out, nuevaRuta);
                        break;
                    case "9":
                        String nombre = in.readUTF();
                        File file = new File(rutaDirectorioRemoto + File.separator + nombre);
                        if (!file.exists()) {
                            if (nombre.contains(".")) {
                                file.createNewFile();
                                recibirArchivoRemoto(in, file);
                            } else {
                                file.mkdir();
                                recibirCarpetaRemota(in, file);
                            }
                        } else {
                            System.out.println("El archivo o carpeta ya existe");
                        }
                        break;
                    case "10":
                        String nombreRemotoLocal = in.readUTF();
                        File fileR = new File(rutaDirectorioRemoto + File.separator + nombreRemotoLocal);
                        if (fileR.exists()) {
                            if (fileR.isDirectory()) {
                                out.writeUTF("CARPETA:" + fileR.getName());
                                enviarCarpetaRemota(out, fileR);
                            } else {
                                out.writeUTF("ARCHIVO:" + fileR.getName());
                                enviarArchivoRemoto(out, fileR);
                            }
                        } else {
                            out.writeUTF("El archivo o carpeta no existe");
                        }
                        break;
                    case "11":
                        System.out.println("El cliente ha solicitado salir. Cerrando la conexión...");
                        socket.close();
                        return;
                    
                    default:
                        out.writeUTF("Opción no válida");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void mostrarContenidoCarpetaRemota(DataOutputStream out, String ruta, String indentacion) throws IOException {
        File carpetaRemota = new File(ruta);
        File[] archivos = carpetaRemota.listFiles();

        if (archivos != null && archivos.length > 0) {
            for (File archivo : archivos) {
                if (archivo.isDirectory()) {
                    out.writeUTF(indentacion + "[Carpeta] " + archivo.getName());
                    mostrarContenidoCarpetaRemota(out, archivo.getPath(), indentacion + "    ");
                } else {
                    out.writeUTF(indentacion + "[Archivo] " + archivo.getName());
                }
            }
        } else {
            out.writeUTF(indentacion + "La carpeta está vacía o no se encontró");
        }
    }

    private static void crearCarpetaRemota(DataOutputStream out, String nombreCarpeta) throws IOException {
        File nuevaCarpeta = new File(rutaDirectorioRemoto + "\\" + nombreCarpeta);
        if (!nuevaCarpeta.exists()) {
            if (nuevaCarpeta.mkdir()) {
                out.writeUTF("La carpeta '" + nombreCarpeta + "' se creó con éxito.");
            } else {
                out.writeUTF("Error al crear la carpeta '" + nombreCarpeta + "'.");
            }
        } else {
            out.writeUTF("La carpeta '" + nombreCarpeta + "' ya existe.");
        }
    }

    private static void borrarRemota(DataOutputStream out, String nombreArchivo) throws IOException {
        File archivo = new File(rutaDirectorioRemoto + "\\" + nombreArchivo);
        if (archivo.exists()) {
            if (archivo.delete()) {
                out.writeUTF("El archivo/carpeta '" + nombreArchivo + "' se borró con éxito.");
            } else {
                out.writeUTF("Error al borrar el archivo/carpeta '" + nombreArchivo + "'.");
            }
        } else {
            out.writeUTF("El archivo/carpeta '" + nombreArchivo + "' no existe.");
        }
    }

    private static void cambiarRutaDirectorioRemoto(DataOutputStream out, String nuevaRuta) throws IOException {
        File nuevaCarpeta = new File(nuevaRuta);
        if (nuevaCarpeta.exists() && nuevaCarpeta.isDirectory()) {
            rutaDirectorioRemoto = nuevaRuta;
            out.writeUTF("La ruta del directorio remoto se cambió a '" + nuevaRuta + "'.");
        } else {
            out.writeUTF("La ruta proporcionada no existe o no es un directorio.");
        }
    }
    ///////////////RECIBIR DE LOCAL A REMOTO//////////////////
    private static void recibirArchivoRemoto(DataInputStream in, File archivo) throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        Files.write(archivo.toPath(), bytes);
        System.out.println("El archivo fue recibido exitosamente");
    }

    private static void recibirCarpetaRemota(DataInputStream in, File carpeta) throws IOException {
        String nombreArchivo;
        while (!(nombreArchivo = in.readUTF()).equals("FIN")) {
            if (nombreArchivo.startsWith("CARPETA:")) {
                File nuevaCarpeta = new File(carpeta.getPath() + File.separator + nombreArchivo.substring(8));
                nuevaCarpeta.mkdir();
                recibirCarpetaRemota(in, nuevaCarpeta);
            } else if (nombreArchivo.startsWith("ARCHIVO:")) {
                File archivo = new File(carpeta.getPath() + File.separator + nombreArchivo.substring(8));
                recibirArchivoRemoto(in, archivo);
            }
        }
        System.out.println("La carpeta fue recibida exitosamente");
    }
    ///////////////ENVIAR A LOCAL//////////////////
    private static void enviarArchivoRemoto(DataOutputStream out, File archivo) throws IOException {
        byte[] bytes = Files.readAllBytes(archivo.toPath());
        out.writeInt(bytes.length);
        out.write(bytes);
        System.out.println("El archivo fue enviado exitosamente");
    }
    
    private static void enviarCarpetaRemota(DataOutputStream out, File carpeta) throws IOException {
        File[] archivos = carpeta.listFiles();
        for (File archivo : archivos) {
            if (archivo.isDirectory()) {
                out.writeUTF("CARPETA:" + archivo.getName());
                enviarCarpetaRemota(out, archivo);
            } else {
                out.writeUTF("ARCHIVO:" + archivo.getName());
                byte[] bytes = Files.readAllBytes(archivo.toPath());
                out.writeInt(bytes.length);
                out.write(bytes);
            }
        }
        out.writeUTF("FIN");
        System.out.println("La carpeta fue enviada exitosamente");
    } 
}
