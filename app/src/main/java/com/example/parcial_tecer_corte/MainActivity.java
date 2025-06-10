// Archivo: MainActivity.java
package com.example.parcial_tecer_corte;

// Importaciones de Android y OpenCV
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core; // Importación para operaciones de rotación y volteo
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.CLAHE; // Importación para CLAHE
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList; // Importación para ArrayList
import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity {

    // Vistas de la cámara de OpenCV
    CameraBridgeViewBase cameraBridgeViewBase;

    // Clasificador para detección de rostros
    CascadeClassifier cascadeClassifier;

    // Botones de la interfaz de usuario
    private Button btn_galeria; // Usado para modo de detección de monedas
    private Button btn_camara;  // Usado para modo de detección de rostros

    // Contador para cambiar entre modos (1: monedas, 2: rostros)
    int contador;

    // Matrices de OpenCV para procesamiento de imágenes
    Mat gray_; // Para la imagen en escala de grises
    Mat rgb;   // Para la imagen a color (RGBA)

    MatOfRect rects; // Para almacenar rectángulos detectados

    // TextView para mostrar el resultado (suma de monedas)
    private TextView textViewResult;

    // Índice de la cámara actual (se cambiará automáticamente en el click de btn_camara para rostros)
    private int currentCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;

    // --- Clase interna estática para definir la información de cada moneda ---
    private static class CoinInfo {
        public int value;              // El valor de la moneda (ej. 50, 100, 200)
        public int expectedPixelRadius; // El radio esperado de la moneda en PÍXELES (¡Necesita calibración!)

        // Constructor
        public CoinInfo(int value, int expectedPixelRadius) {
            this.value = value;
            this.expectedPixelRadius = expectedPixelRadius;
        }
    }

    // --- Métodos del Ciclo de Vida de la Actividad ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Establece el layout de la actividad

        // Mover la inicialización de textViewResult al principio para evitar NullPointerException
        textViewResult = findViewById(R.id.textView2); // Enlaza el TextView del layout

        // Inicializa la librería OpenCV
        if (OpenCVLoader.initDebug()) {
            Toast.makeText(MainActivity.this, "Libreria OpenCV instalada correctamente", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "Libreria OpenCV instalada incorrectamente", Toast.LENGTH_LONG).show();
        }

        getPermission(); // Solicita permisos de la cámara

        // Configura la vista de la cámara de OpenCV
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        // Establece la cámara inicial (trasera por defecto al inicio de la app)
        cameraBridgeViewBase.setCameraIndex(currentCameraIndex);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                // Inicializa las matrices Mat cuando la vista de la cámara se inicia
                rgb = new Mat();
                gray_ = new Mat();
                rects = new MatOfRect();
            }

            @Override
            public void onCameraViewStopped() {
                // Libera los recursos de las matrices Mat cuando la vista de la cámara se detiene
                rgb.release();
                gray_.release();
                rects.release();
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                // Este método se llama para cada fotograma de la cámara
                rgb = inputFrame.rgba(); // Obtiene el fotograma a color (RGBA)
                gray_ = inputFrame.gray(); // Obtiene el fotograma en escala de grises

                // --- Corrección de rotación y reflejo de la cámara frontal ---
                // Este bloque de código intenta corregir la orientación y el reflejo (efecto espejo)
                // de la imagen de la cámara frontal, que a menudo viene invertida o reflejada.
                //if (currentCameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT) {
                    // PASO 1: Rotar 180 grados. Esto corrige si la imagen viene patas arriba.
                    // Algunos dispositivos necesitan esto, otros no.
                  //  Core.rotate(rgb, rgb, Core.ROTATE_90_COUNTERCLOCKWISE);
                    //Core.rotate(gray_, gray_, Core.ROTATE_90_CLOCKWISE);

                    // PASO 2: Voltear horizontalmente (efecto espejo). Esto corrige el reflejo lateral.
                    // La mayoría de las cámaras frontales reflejan la imagen para que se vea como un espejo.
                    // Core.flip(src, dst, 1) realiza un volteo horizontal.
                    // Si la imagen **ya no está reflejada** y este paso la refleja, puedes **COMENTAR** estas dos líneas.
                    // Si la imagen está **patas arriba** (volteo vertical), puedes probar Core.flip(rgb, rgb, 0);
                    // Si la imagen necesita volteo en **ambos ejes**, puedes probar Core.flip(rgb, rgb, -1);
                    //Core.flip(rgb, rgb, 1); // Prueba inicial para corregir reflejo horizontal
                    //Core.flip(gray_, gray_, 1);
                //}
                // Nota: Si la cámara trasera también se rota incorrectamente, se podría añadir aquí
                // Core.rotate(..., Core.ROTATE_90_CLOCKWISE) o ROTATE_270_CLOCKWISE
                // y Core.flip(..., ...) según sea necesario.

                // Lógica para alternar entre modos de detección
                if (contador == 1) {
                    // Modo de detección de monedas
                    detec_moneda(rgb);
                } else if (contador == 2) {
                    // Modo de detección de rostros (Haar Cascade)
                    Imgproc.equalizeHist(gray_, gray_); // Ecualiza el histograma para mejorar el contraste

                    MatOfRect detectedObjects = new MatOfRect();
                    // Detecta objetos (rostros) usando el clasificador cargado
                    if (cascadeClassifier != null) {
                        // param1 (1.1): Factor de escala. Reduce la imagen para la detección.
                        // param2 (3): Mínimo de vecinos para considerar una detección.
                        cascadeClassifier.detectMultiScale(gray_, detectedObjects, 1.1, 3);
                    }

                    // Itera sobre los objetos detectados y dibuja elipses alrededor de los rostros
                    for (Rect rect : detectedObjects.toArray()) {
                        // Calcula el punto central del rostro
                        Point center = new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
                        // Dibuja una elipse magenta alrededor del rostro
                        Imgproc.ellipse(rgb, center, new Size(rect.width / 2, rect.height / 2),
                                0, 0, 360, new Scalar(255, 0, 255), 4);
                        // Dibuja un texto "Rostro" encima de la detección
                        Imgproc.putText(rgb, "Rostro", new Point(rect.x, rect.y - 10),
                                Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, new Scalar(255, 0, 255), 2);
                    }
                }

                // Devuelve la matriz RGB (con los dibujos si los hay) para que se muestre en la vista de la cámara
                return rgb;
            }
        });

        // Configuración de los botones
        btn_galeria = findViewById(R.id.button3);
        btn_galeria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contador = 1; // Establece el modo a detección de monedas

                // Asegura que la cámara trasera esté activa al entrar en modo moneda
                currentCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
                cameraBridgeViewBase.disableView();
                cameraBridgeViewBase.setCameraIndex(currentCameraIndex);
                cameraBridgeViewBase.enableView();

                Toast.makeText(MainActivity.this, "Modo: Detección de Monedas (Cámara Trasera)", Toast.LENGTH_SHORT).show();
                // Opcional: limpiar el texto del resultado si se cambia de modo
                if (textViewResult != null) {
                    runOnUiThread(() -> textViewResult.setText("El valor es: 0 COP"));
                }
            }
        });

        btn_camara = findViewById(R.id.button4);
        btn_camara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contador = 2; // Establece el modo a detección de rostros

                // --- COMPORTAMIENTO AUTOMÁTICO: Cambiar a la cámara frontal ---
                currentCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
                cameraBridgeViewBase.disableView(); // Deshabilita la vista para el cambio de cámara
                cameraBridgeViewBase.setCameraIndex(currentCameraIndex); // Establece la cámara frontal
                cameraBridgeViewBase.enableView(); // Habilita la vista con la nueva cámara

                Toast.makeText(MainActivity.this, "Modo: Detección de Rostros (Cámara Frontal)", Toast.LENGTH_LONG).show();
                // Opcional: limpiar el texto del resultado si se cambia de modo
                if (textViewResult != null) {
                    runOnUiThread(() -> textViewResult.setText(""));
                }

                // Carga el clasificador Haar Cascade para la detección de rostros (haarcascade_frontalface_alt.xml)
                try {
                    // Usamos haarcascade_frontalface_alt.xml para detección de rostros
                    InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                    File file = new File(getDir("cascade", MODE_PRIVATE), "haarcascade_frontalface_alt.xml");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    byte[] data = new byte[1179648]; // Tamaño buffer
                    int read_bytes;

                    // Lee el archivo del recurso y lo escribe en un archivo temporal
                    while ((read_bytes = inputStream.read(data)) != -1) {
                        fileOutputStream.write(data, 0, read_bytes);
                    }

                    // Carga el clasificador desde el archivo temporal
                    cascadeClassifier = new CascadeClassifier(file.getAbsolutePath());
                    if (cascadeClassifier.empty()) {
                        cascadeClassifier = null; // Si falla la carga, establece a null
                        Toast.makeText(MainActivity.this, "Error al cargar clasificador de rostros", Toast.LENGTH_LONG).show();
                    }

                    // Cierra streams y elimina el archivo temporal
                    inputStream.close();
                    fileOutputStream.close();
                    file.delete();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Archivo clasificador de rostros no encontrado", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error de E/S al cargar clasificador de rostros", Toast.LENGTH_LONG).show();
                }
            }
        });
        // El botón 'btn_switch_camera' no se inicializa aquí y no tiene un OnClickListener.
        // Si lo tenías en tu activity_main.xml, puedes eliminarlo, o simplemente no se usará.
    }

    // --- Métodos de Permisos y Cámara ---

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        // Retorna la lista de vistas de cámara que usará OpenCV
        return Collections.singletonList(cameraBridgeViewBase);
    }

    void getPermission() {
        // Solicita el permiso de la cámara si no ha sido concedido
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Maneja el resultado de la solicitud de permisos
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission(); // Si no se concedió, vuelve a pedir
        }
    }

    // --- Método de Detección de Monedas (SIN CAMBIOS EN SU LÓGICA INTERNA) ---
    void detec_moneda(Mat img) {
        // img es el frame de la cámara en formato RGBA (color)

        // Convertir a escala de grises para el procesamiento
        Mat grayImg = new Mat();
        Imgproc.cvtColor(img, grayImg, Imgproc.COLOR_BGR2GRAY);

        // --- Mejora de Iluminación: Aplicar CLAHE ---
        // CLAHE (Contrast Limited Adaptive Histogram Equalization)
        CLAHE clahe = Imgproc.createCLAHE();
        clahe.setClipLimit(2.5);
        clahe.setTilesGridSize(new Size(8, 8));
        clahe.apply(grayImg, grayImg);

        // --- Aplicar filtro Gaussiano ---
        Imgproc.GaussianBlur(grayImg, grayImg, new Size(9, 9), 2, 2);

        // Detectar círculos con la transformada de Hough
        Mat circles = new Mat();

        // --- Parámetros de HoughCircles (AJUSTAR SEGÚN CALIBRACIÓN REAL) ---
        double dp = 1;
        double minDist = grayImg.rows() / 8;
        double param1 = 100;
        double param2 = 75;
        int minRadiusGlobal = 50;
        int maxRadiusGlobal = 100;

        Imgproc.HoughCircles(grayImg, circles, Imgproc.HOUGH_GRADIENT, dp,
                minDist, param1, param2, minRadiusGlobal, maxRadiusGlobal);

        // --- Definición de las monedas y sus radios ESPERADOS (EN PÍXELES) ---
        // ESTOS VALORES 'expectedPixelRadius' SON EJEMPLOS Y NECESITAN CALIBRACIÓN.
        List<CoinInfo> coinDefinitions = new ArrayList<>();
        coinDefinitions.add(new CoinInfo(50, 40));   // 50 pesos, aprox. 40 píxeles de radio
        coinDefinitions.add(new CoinInfo(100, 55));  // 100 pesos, aprox. 55 píxeles de radio
        coinDefinitions.add(new CoinInfo(200, 65));  // 200 pesos, aprox. 65 píxeles de radio
        coinDefinitions.add(new CoinInfo(500, 74));  // 500 pesos, aprox. 74 píxeles de radio
        coinDefinitions.add(new CoinInfo(1000, 95)); // 1000 pesos, aprox. 95 píxeles de radio

        IntWrapper totalDineroWrapper = new IntWrapper(0); // Para la suma total

        // Dibujar los círculos detectados y clasificar su valor
        if (circles.cols() > 0) {
            for (int i = 0; i < circles.cols(); i++) {
                double[] circle = circles.get(0, i);
                Point center = new Point(Math.round(circle[0]), Math.round(circle[1]));
                int detectedRadius = (int) Math.round(circle[2]);

                double valorMoneda = 0.0;
                double minDiff = Double.MAX_VALUE;
                double tolerance = 5.0; // Tolerancia en píxeles para la coincidencia del radio

                for (CoinInfo coin : coinDefinitions) {
                    double diff = Math.abs(detectedRadius - coin.expectedPixelRadius);
                    if (diff < minDiff && diff < tolerance) {
                        minDiff = diff;
                        valorMoneda = coin.value;
                    }
                }

                if (valorMoneda > 0) {
                    totalDineroWrapper.value += valorMoneda;

                    Imgproc.circle(img, center, detectedRadius, new Scalar(0, 255, 0), 2); // Círculo verde
                    String text = (int) valorMoneda + " COP";
                    Imgproc.putText(img, text, new Point(center.x + 10, center.y),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 0, 0), 2); // Texto azul
                } else {
                    Imgproc.circle(img, center, detectedRadius, new Scalar(0, 0, 255), 2); // Círculos no identificados en rojo
                }
            }
        }

        // Mostrar el total de dinero
        String textoTotal = "Total: " + totalDineroWrapper.value + " COP";
        Imgproc.putText(img, textoTotal, new Point(10, 50),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(0, 255, 0), 3); // Texto verde

        runOnUiThread(() -> textViewResult.setText(textoTotal));
    }
}
