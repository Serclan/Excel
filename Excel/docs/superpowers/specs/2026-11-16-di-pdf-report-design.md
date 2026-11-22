# Diseño: informes PDF deportivos para DI

Fecha: 2026-11-16
Proyecto: `E:\NetBeansProjects\Excel`

## Objetivo
Ampliar la aplicación deportiva existente para que permita generar informes PDF individuales a partir de los mismos datos y cálculos que usa la exportación Excel. El cambio se limita a los requisitos de la segunda parte del proyecto; el tiempo de tutoría seguirá dedicado al contenido de examen.

## Diseño propuesto
- Mantener la ventana Swing `Excel` y añadir una acción para generar el PDF. Permitirá elegir deportista, fecha inicial/final y destino del archivo, y comprobará que el rango es válido y contiene datos suficientes.
- Añadir un generador separado (`GeneradorPDF`) con `org.apache.pdfbox:pdfbox-app:3.0.1`, exactamente como muestran los apuntes. Recibirá el deportista y sus datos filtrados; no duplicará ni alterará los cálculos de `CalculosDeportivos`.
- Crear un informe de 3–5 páginas con portada, resumen, tabla semanal, tabla e historial de pruebas, gráficos de evolución de 5 km y carga semanal, interpretación prudente, imágenes proporcionadas o una representación genérica si faltan, y encabezado/pie con paginación.
- Gestionar datos insuficientes, rutas inválidas y errores al guardar mediante mensajes comprensibles en Swing.
- Añadir datos de muestra para un segundo deportista, de modo que se pueda generar y entregar un ejemplo distinto para dos perfiles.
- Entregables: proyecto Java integrado, PDF de ejemplo, explicación de hasta una página y evidencia de generación para dos deportistas.

## Alternativas consideradas
1. Integrar PDFBox en `Excel.java`: menos archivos, pero mezcla interfaz y maquetación; descartado.
2. Generar un PDF estático independiente: más rápido, pero no reutiliza los datos ni demuestra que el informe sea dinámico; descartado.
3. Generador separado invocado desde Swing: mantiene responsabilidades claras y permite reutilizar la aplicación; recomendado.

## Límites y supuestos
- Se conserva el modelo y la lógica existente; no se rehace la exportación Excel.
- La primera parte ya contiene muestras de entrenamientos y pruebas para DEP01; se completará al menos DEP02 con datos ficticios coherentes para las dos demostraciones.
- La entrega no incluirá datos personales reales. Si no hay fotografía del atleta, se usará una ilustración genérica no identificativa.
- El proyecto no contiene actualmente control de selección de fechas o informes PDF; se añadirá solo lo necesario para esta entrega.

## Revisión rápida
El alcance cubre los bloques de contenido, elementos visuales, requisitos técnicos y entregables descritos en `Segunda_parte_practica_informes_PDF_deportivos.pdf`. La generación queda conectada al modelo existente y los datos de muestra permiten mostrar dos informes dinámicos. La estructura de 3–5 páginas tendrá saltos de página controlados y será comprobada visualmente antes de entregar.

