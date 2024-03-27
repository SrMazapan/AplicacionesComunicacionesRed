import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Scanner;

public class Cliente {
    private static String rutaDirectorioLocal = "C:\\Users\\jemma\\Documents\\local";
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws InterruptedException {
        try (Socket socket = new Socket("localhost", 8080);//conexion con servidor
             DataInputStream in = new DataInputStream(socket.getInputStream());//flujo de entrada
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {//flujo salida

            String opcion;
            while (true) {
                mostrarMenu();
                opcion = scanner.nextLine();
                out.writeUTF(opcion);

                switch (opcion) {
                    case "1":
                        mostrarContenidoCarpetaLocal();
                        break;
                    case "2":
                        crearCarpetaLocal();
                        break;
                    case "3":
                        borrarArchivoLocal();
                        break;
                    case "4":
                        cambiarRutaLocal();
                        break;
                    case "5":
                        Thread.sleep(1000);
                        break;
                    case "6":
                        // Crear carpeta remota
                        System.out.println("Introduce el nombre de la carpeta que quieres crear:");
                        String nombreCarpeta = scanner.nextLine();
                        out.writeUTF(nombreCarpeta);
                        break;
                    case "7":
                        // Crear carpeta remota
                        System.out.println("Introduce el nombre del archivo o carpeta que quieres borrar:");
                        String nombreArchivo = scanner.nextLine();
                        out.writeUTF(nombreArchivo);
                        break;
                    case "8":
                        System.out.println("Introduce la nueva ruta del directorio remoto:");
                        String nuevaRuta = scanner.nextLine();
                        out.writeUTF(nuevaRuta);
                        break;
                    case "9":
                        System.out.print("Introduce el nombre del archivo o carpeta a enviar: ");
                        String nombre = scanner.nextLine();
                        //creación objeto file que representa la ruta completa del archivo/carpeta
                        File file = new File(rutaDirectorioLocal + File.separator + nombre);
                        if (file.exists()) {
                            out.writeUTF(nombre);
                            if (file.isDirectory()) {
                                enviarCarpetaLocal(out, file);
                            } else {
                                enviarArchivoLocal(out, file);
                            }
                        } else {
                            System.out.println("El archivo o carpeta no existe");
                        }
                        break;
                    case "10":
                        System.out.print("Introduce el nombre del archivo o carpeta a recibir: ");
                        String nombreRemotoLocal = scanner.nextLine();
                        out.writeUTF(nombreRemotoLocal);
                        //recepción del elemento
                        String tipo = in.readUTF();
                        if (tipo.startsWith("CARPETA:")) {
                            //crea objeto tipo file representando la carpeta
                            File nuevaCarpeta = new File(rutaDirectorioLocal + File.separator + tipo.substring(8));
                            //crea dicha carpeta
                            nuevaCarpeta.mkdir();
                            recibirCarpetaLocal(in, nuevaCarpeta);
                        } else if (tipo.startsWith("ARCHIVO:")) {
                            File archivo = new File(rutaDirectorioLocal + File.separator + tipo.substring(8));
                            recibirArchivoLocal(in, archivo);
                        }
                        break;
                    case "11":
                        out.writeUTF("SALIR");
                        System.out.println("Saliendo del programa...");
                        socket.close();
                        return;
                    default:
                        System.out.println(in.readUTF());
                        break;
                }

                waitForResponse(in);              
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void mostrarMenu() {
        System.out.println("1. Listar el contenido de la carpeta local");
        System.out.println("2. Crear carpeta localmente");
        System.out.println("3. Borrar archivos de la carpeta local");
        System.out.println("4. Cambiar ruta del directorio local");
        System.out.println("5. Listar el contenido de la carpeta remota");
        System.out.println("6. Crear carpetas remotamente");
        System.out.println("7. Borrar archivos de la carpeta remota");
        System.out.println("8. Cambiar ruta del directorio remoto");
        System.out.println("9. Enviar archivos/carpetas desde la carpeta local hacia la remota");
        System.out.println("10. Enviar archivos/carpetas desde la carpeta remota hacia la local");
        System.out.println("11. Salir");
        System.out.print("Elige una opción: ");
    }

    private static void mostrarContenidoCarpetaLocal() {
        //llamamos pasando el objeto File del directorio local y el nivel de nuestro árbol
        mostrarArbolDirectorio(new File(rutaDirectorioLocal), 0);
    }

    private static void mostrarArbolDirectorio(File dir, int nivel) {
        //obtiene lista de directorios y archivos dentro del dir. dado.
        File[] files = dir.listFiles();
        //itera sobre cada elemento     
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    System.out.println(getIndent(nivel) + "[Carpeta] " + file.getName());
                    //recursividad para mostrar contenido del subdirectorio.
                    mostrarArbolDirectorio(file, nivel + 1);
                } else {
                    System.out.println(getIndent(nivel) + file.getName());
                }
            }
        } else {
            System.out.println(getIndent(nivel) + "El directorio está vacío o no se encontró");
        }
    }

    private static String getIndent(int nivel) {
        //almacena los espacios en blanco
        StringBuilder indent = new StringBuilder();
        //bucle para agregar dichos espacios en blanco
        for (int i = 0; i < nivel; i++) {
            indent.append("  ");
        }
        //devolvemos la cadena de espacios en blanco
        return indent.toString();
    }

    private static void crearCarpetaLocal() {
        System.out.print("Introduce el nombre de la nueva carpeta: ");
        String nombreCarpeta = scanner.nextLine();
        File nuevaCarpeta = new File(rutaDirectorioLocal + File.separator + nombreCarpeta);
        boolean creado = nuevaCarpeta.mkdirs();
        if (creado) {
            System.out.println("La carpeta '" + nombreCarpeta + "' se creó con éxito.");
        } else {
            System.out.println("No se pudo crear la carpeta '" + nombreCarpeta + "'.");
        }
    }

    private static void borrarArchivoLocal() {
        System.out.print("Introduce el nombre del archivo o carpeta a borrar: ");
        String nombreArchivo = scanner.nextLine();
        File archivo = new File(rutaDirectorioLocal + File.separator + nombreArchivo);
        if (archivo.exists()) {
            boolean borrado = borrarRecursivo(archivo);
            if (borrado) {
                System.out.println("El archivo o carpeta fue borrado exitosamente");
            } else {
                System.out.println("No se pudo borrar el archivo o carpeta");
            }
        } else {
            System.out.println("El archivo o carpeta especificado no existe");
        }
    }

    private static boolean borrarRecursivo(File archivo) {
        if (archivo.isDirectory()) {
            File[] archivos = archivo.listFiles();
            if (archivos != null) {
                for (File file : archivos) {
                    borrarRecursivo(file);
                }
            }
        }
        return archivo.delete();
    }

    private static void cambiarRutaLocal() {
        System.out.print("Introduce la nueva ruta del directorio: ");
        rutaDirectorioLocal = scanner.nextLine();
        System.out.println("La ruta del directorio local ha sido cambiada a: " + rutaDirectorioLocal);
    }

    private static void waitForResponse(DataInputStream in) throws IOException {
        String respuesta;
        //Espera hasta que haya datos disponibles en el flujo de entrada, y que la respuesta del servidor no sea "FIN"
        while (in.available() > 0 && !(respuesta = in.readUTF()).equals("FIN")) {
            System.out.println(respuesta);
        }
    }

    //////////////////ENVIAR DE LOCAL A REMOTO/////////////////////////
    private static void enviarArchivoLocal(DataOutputStream out, File archivo) throws IOException {
        //leemos los bytes del archivo
        byte[] bytes = Files.readAllBytes(archivo.toPath());
        //escribe la longitud de los bytes en el flujo de salida
        out.writeInt(bytes.length);
        //escribe los bytes del archivo en el flujo de salida
        out.write(bytes);
        System.out.println("El archivo fue enviado exitosamente");
    }

    private static void enviarCarpetaLocal(DataOutputStream out, File carpeta) throws IOException {
        //obtenemos la lista de archivos dentro de la carpeta
        File[] archivos = carpeta.listFiles();
        for (File archivo : archivos) {
            if (archivo.isDirectory()) {
                out.writeUTF("CARPETA:" + archivo.getName());
                enviarCarpetaLocal(out, archivo);
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
    //////////////////RECIBIR DE REMOTO/////////////////////////
    private static void recibirArchivoLocal(DataInputStream in, File archivo) throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        //escribe los bytes en un nuevo archivo en el local
        Files.write(archivo.toPath(), bytes);
        System.out.println("El archivo fue recibido exitosamente");
    }
    
    private static void recibirCarpetaLocal(DataInputStream in, File carpeta) throws IOException {
        String nombreArchivo;
        //leemos el nombre del archivo desde el flujo de entrada, y mientras que no sea FIN, continua
        while (!(nombreArchivo = in.readUTF()).equals("FIN")) {
            if (nombreArchivo.startsWith("CARPETA:")) {
                File nuevaCarpeta = new File(carpeta.getPath() + File.separator + nombreArchivo.substring(8));
                nuevaCarpeta.mkdir();
                recibirCarpetaLocal(in, nuevaCarpeta);
            } else if (nombreArchivo.startsWith("ARCHIVO:")) {
                File archivo = new File(carpeta.getPath() + File.separator + nombreArchivo.substring(8));
                recibirArchivoLocal(in, archivo);
            }
        }
        System.out.println("La carpeta fue recibida exitosamente");
    }

}
