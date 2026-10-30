# -*- coding: utf-8 -*-
"""
Genera dos capturas fotorrealistas de Atlassian Jira para el Sprint 3:
1. Burnup Report (SCRUM Sprint 3)
2. Burndown Report (SCRUM Sprint 3)
Renderizados en ultra-alta definicion con Google Chrome headless.
"""

import os
import shutil
import subprocess

OUTPUT_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\diagramas"
ASSETS_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\sprint3_assets"
DOWNLOADS_DIR = r"C:\Users\ASUS\Downloads"
os.makedirs(OUTPUT_DIR, exist_ok=True)
os.makedirs(ASSETS_DIR, exist_ok=True)
os.makedirs(DOWNLOADS_DIR, exist_ok=True)

CHROME_PATH = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

# ==============================================================================
# 1. HTML PARA EL BURNUP REPORT
# ==============================================================================
burnup_html = """<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<style>
  * {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  }
  html, body {
    background-color: #ffffff;
    padding: 24px 32px 24px 32px;
    width: 1040px;
    overflow: hidden;
  }
  .title {
    font-size: 21px;
    font-weight: 600;
    color: #172b4d;
    margin-bottom: 16px;
  }
  /* Controls row */
  .controls-row {
    display: flex;
    align-items: flex-end;
    gap: 16px;
    margin-bottom: 14px;
    position: relative;
  }
  .control-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .control-label {
    font-size: 11px;
    color: #6b778c;
    font-weight: 600;
  }
  .dropdown {
    height: 32px;
    border: 1px solid #c1c7d0;
    border-radius: 3px;
    padding: 0 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    color: #172b4d;
    background: #ffffff;
    min-width: 175px;
    cursor: pointer;
  }
  .dropdown.active {
    border-color: #4c9aff;
    box-shadow: 0 0 0 1px #4c9aff;
  }
  .dropdown .caret {
    font-size: 10px;
    color: #42526e;
    margin-left: 8px;
  }
  .dots-menu {
    position: absolute;
    right: 0;
    bottom: 4px;
    border: 1px solid #dfe1e6;
    border-radius: 3px;
    padding: 4px 8px;
    font-size: 12px;
    color: #6b778c;
    letter-spacing: 1px;
    background: #ffffff;
  }
  /* Date row */
  .date-row {
    font-size: 11.5px;
    color: #172b4d;
    margin-bottom: 12px;
  }
  .date-row strong {
    font-weight: 700;
  }
  /* Legend */
  .legend-row {
    display: flex;
    gap: 40px;
    margin-bottom: 16px;
  }
  .legend-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
  }
  .legend-line {
    width: 28px;
    height: 3px;
    margin-top: 6px;
    flex-shrink: 0;
  }
  .line-green { background-color: #68bc00; }
  .line-gray { background-color: #8993a4; }
  .line-red { background-color: #de350b; }
  .legend-text {
    display: flex;
    flex-direction: column;
  }
  .legend-title {
    font-size: 11px;
    font-weight: 700;
    color: #172b4d;
  }
  .legend-desc {
    font-size: 10px;
    color: #6b778c;
  }
  /* Chart area */
  .chart-wrapper {
    position: relative;
    width: 980px;
    height: 300px;
    margin-top: 10px;
  }
  .y-axis-label {
    position: absolute;
    left: -70px;
    top: 130px;
    transform: rotate(-90deg);
    font-size: 11px;
    color: #6b778c;
    font-weight: 500;
    white-space: nowrap;
  }
  svg {
    width: 100%;
    height: 100%;
    overflow: visible;
  }
</style>
</head>
<body>

<div class="title">Burnup report</div>

<div class="controls-row">
  <div class="control-group">
    <div class="control-label">Sprint</div>
    <div class="dropdown">
      <span>SCRUM Sprint 3</span>
      <span class="caret">&#9660;</span>
    </div>
  </div>
  <div class="control-group">
    <div class="control-label">Campo de estimación</div>
    <div class="dropdown active">
      <span>Recuento de actividades</span>
      <span class="caret">&#9660;</span>
    </div>
  </div>
  <div class="dots-menu">...</div>
</div>

<div class="date-row">
  <strong>Fecha -</strong> 5 de octubre de 2026 - 26 de octubre de 2026
</div>

<div class="legend-row">
  <div class="legend-item">
    <div class="legend-line line-green"></div>
    <div class="legend-text">
      <span class="legend-title">Trabajo completado</span>
      <span class="legend-desc">Número de actividades que han completado este sprint</span>
    </div>
  </div>
  <div class="legend-item">
    <div class="legend-line line-gray"></div>
    <div class="legend-text">
      <span class="legend-title">Directriz</span>
      <span class="legend-desc">Tasa de evolución ideal</span>
    </div>
  </div>
  <div class="legend-item">
    <div class="legend-line line-red"></div>
    <div class="legend-text">
      <span class="legend-title">Alcance del trabajo</span>
      <span class="legend-desc">Número de actividades para completar este sprint</span>
    </div>
  </div>
</div>

<div class="chart-wrapper">
  <div class="y-axis-label">Recuento de actividades</div>
  <svg viewBox="0 0 980 290">
    <!-- Grid horizontal lines (Y from 0 to 6) -->
    <!-- Plot box: X=40 to 960 (width 920), Y=20 (val 6) to 250 (val 0) (height 230, 38.33 px per unit) -->
    <line x1="40" y1="20" x2="960" y2="20" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="24" font-size="11" fill="#6b778c" text-anchor="end">6</text>

    <line x1="40" y1="58.33" x2="960" y2="58.33" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="62.33" font-size="11" fill="#6b778c" text-anchor="end">5</text>

    <line x1="40" y1="96.66" x2="960" y2="96.66" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="100.66" font-size="11" fill="#6b778c" text-anchor="end">4</text>

    <line x1="40" y1="135" x2="960" y2="135" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="139" font-size="11" fill="#6b778c" text-anchor="end">3</text>

    <line x1="40" y1="173.33" x2="960" y2="173.33" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="177.33" font-size="11" fill="#6b778c" text-anchor="end">2</text>

    <line x1="40" y1="211.66" x2="960" y2="211.66" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="215.66" font-size="11" fill="#6b778c" text-anchor="end">1</text>

    <line x1="40" y1="250" x2="960" y2="250" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="254" font-size="11" fill="#6b778c" text-anchor="end">0</text>

    <!-- Outer frame / Axes -->
    <line x1="40" y1="20" x2="40" y2="250" stroke="#c1c7d0" stroke-width="1.2"/>
    <line x1="960" y1="20" x2="960" y2="250" stroke="#c1c7d0" stroke-width="1.2"/>
    <line x1="40" y1="250" x2="960" y2="250" stroke="#c1c7d0" stroke-width="1.2"/>
    <line x1="40" y1="20" x2="960" y2="20" stroke="#c1c7d0" stroke-width="1.2"/>

    <!-- X-axis Ticks & Dates (11 intervals from 05 oct to 26 oct, every 2 days, step = 92px) -->
    <!-- X0=40 (05 oct), X1=132 (07 oct), X2=224 (09 oct), X3=316 (11 oct), X4=408 (13 oct), X5=500 (15 oct), X6=592 (17 oct), X7=684 (19 oct), X8=776 (21 oct), X9=868 (23 oct), X10=960 (26 oct) -->
    <text x="40" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">05 oct</text>
    <text x="132" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">07 oct</text>
    <text x="224" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">09 oct</text>
    <text x="316" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">11 oct</text>
    <text x="408" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">13 oct</text>
    <text x="500" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">15 oct</text>
    <text x="592" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">17 oct</text>
    <text x="684" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">19 oct</text>
    <text x="776" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">21 oct</text>
    <text x="868" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">23 oct</text>
    <text x="960" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">26 oct</text>

    <!-- 1. Alcance del trabajo (Red line: constant at Y=20, scope=6) -->
    <line x1="40" y1="20" x2="960" y2="20" stroke="#de350b" stroke-width="2.5"/>

    <!-- 2. Directriz (Ideal rate: gray line from (40, 250) to (960, 20)) -->
    <line x1="40" y1="250" x2="960" y2="20" stroke="#8993a4" stroke-width="1.8"/>

    <!-- 3. Trabajo completado (Green staircase line reaching 6 on 26 oct) -->
    <!-- 05 oct - 11 oct: 0 (Y=250) -->
    <!-- 11 oct: step to 1 (Y=211.66) -->
    <!-- 14 oct: step to 2 (Y=173.33) -->
    <!-- 17 oct: step to 3 (Y=135) -->
    <!-- 20 oct: step to 4 (Y=96.66) -->
    <!-- 23 oct: step to 5 (Y=58.33) -->
    <!-- 26 oct: step to 6 (Y=20) -->
    <path d="M 40 250
             L 316 250
             L 316 211.66
             L 454 211.66
             L 454 173.33
             L 592 173.33
             L 592 135
             L 730 135
             L 730 96.66
             L 868 96.66
             L 868 58.33
             L 955 58.33
             L 955 20
             L 960 20"
          fill="none" stroke="#68bc00" stroke-width="2.6" stroke-linejoin="miter"/>
  </svg>
</div>

</body>
</html>
"""

# ==============================================================================
# 2. HTML PARA EL BURNDOWN REPORT
# ==============================================================================
burndown_html = """<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<style>
  * {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  }
  html, body {
    background-color: #ffffff;
    padding: 24px 32px 24px 32px;
    width: 1040px;
    overflow: hidden;
  }
  .title {
    font-size: 21px;
    font-weight: 600;
    color: #172b4d;
    margin-bottom: 16px;
  }
  /* Controls row */
  .controls-row {
    display: flex;
    align-items: flex-end;
    gap: 16px;
    margin-bottom: 14px;
    position: relative;
  }
  .control-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .control-label {
    font-size: 11px;
    color: #6b778c;
    font-weight: 600;
  }
  .dropdown {
    height: 32px;
    border: 1px solid #c1c7d0;
    border-radius: 3px;
    padding: 0 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    color: #172b4d;
    background: #ffffff;
    min-width: 175px;
    cursor: pointer;
  }
  .dropdown.active {
    border-color: #4c9aff;
    box-shadow: 0 0 0 1px #4c9aff;
  }
  .dropdown .caret {
    font-size: 10px;
    color: #42526e;
    margin-left: 8px;
  }
  .dots-menu {
    position: absolute;
    right: 0;
    bottom: 4px;
    border: 1px solid #dfe1e6;
    border-radius: 3px;
    padding: 4px 8px;
    font-size: 12px;
    color: #6b778c;
    letter-spacing: 1px;
    background: #ffffff;
  }
  /* Date row */
  .date-row {
    font-size: 11.5px;
    color: #172b4d;
    margin-bottom: 12px;
  }
  .date-row strong {
    font-weight: 700;
  }
  /* Legend */
  .legend-row {
    display: flex;
    gap: 40px;
    margin-bottom: 16px;
  }
  .legend-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
  }
  .legend-line {
    width: 28px;
    height: 3px;
    margin-top: 6px;
    flex-shrink: 0;
  }
  .line-red { background-color: #de350b; }
  .line-gray { background-color: #8993a4; }
  .legend-text {
    display: flex;
    flex-direction: column;
  }
  .legend-title {
    font-size: 11px;
    font-weight: 700;
    color: #172b4d;
  }
  .legend-desc {
    font-size: 10px;
    color: #6b778c;
  }
  /* Chart area */
  .chart-wrapper {
    position: relative;
    width: 980px;
    height: 300px;
    margin-top: 10px;
  }
  .y-axis-label {
    position: absolute;
    left: -70px;
    top: 130px;
    transform: rotate(-90deg);
    font-size: 11px;
    color: #6b778c;
    font-weight: 500;
    white-space: nowrap;
  }
  svg {
    width: 100%;
    height: 100%;
    overflow: visible;
  }
</style>
</head>
<body>

<div class="title">Burndown report</div>

<div class="controls-row">
  <div class="control-group">
    <div class="control-label">Sprint</div>
    <div class="dropdown">
      <span>SCRUM Sprint 3</span>
      <span class="caret">&#9660;</span>
    </div>
  </div>
  <div class="control-group">
    <div class="control-label">Campo de estimación</div>
    <div class="dropdown active">
      <span>Recuento de actividades</span>
      <span class="caret">&#9660;</span>
    </div>
  </div>
  <div class="dots-menu">...</div>
</div>

<div class="date-row">
  <strong>Fecha -</strong> 5 de octubre de 2026 - 26 de octubre de 2026
</div>

<div class="legend-row">
  <div class="legend-item">
    <div class="legend-line line-red"></div>
    <div class="legend-text">
      <span class="legend-title">Trabajo restante</span>
      <span class="legend-desc">Número de actividades que quedan para completar este sprint</span>
    </div>
  </div>
  <div class="legend-item">
    <div class="legend-line line-gray"></div>
    <div class="legend-text">
      <span class="legend-title">Línea de Guía</span>
      <span class="legend-desc">Tasa de evolución ideal</span>
    </div>
  </div>
</div>

<div class="chart-wrapper">
  <div class="y-axis-label">Recuento de actividades</div>
  <svg viewBox="0 0 980 290">
    <!-- Grid horizontal lines (Y from 0 to 6) -->
    <!-- Plot box: X=40 to 960 (width 920), Y=20 (val 6) to 250 (val 0) (height 230, 38.33 px per unit) -->
    <line x1="40" y1="20" x2="960" y2="20" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="24" font-size="11" fill="#6b778c" text-anchor="end">6</text>

    <line x1="40" y1="58.33" x2="960" y2="58.33" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="62.33" font-size="11" fill="#6b778c" text-anchor="end">5</text>

    <line x1="40" y1="96.66" x2="960" y2="96.66" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="100.66" font-size="11" fill="#6b778c" text-anchor="end">4</text>

    <line x1="40" y1="135" x2="960" y2="135" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="139" font-size="11" fill="#6b778c" text-anchor="end">3</text>

    <line x1="40" y1="173.33" x2="960" y2="173.33" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="177.33" font-size="11" fill="#6b778c" text-anchor="end">2</text>

    <line x1="40" y1="211.66" x2="960" y2="211.66" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="215.66" font-size="11" fill="#6b778c" text-anchor="end">1</text>

    <line x1="40" y1="250" x2="960" y2="250" stroke="#ebecf0" stroke-width="1"/>
    <text x="30" y="254" font-size="11" fill="#6b778c" text-anchor="end">0</text>

    <!-- Outer frame / Axes -->
    <line x1="40" y1="20" x2="40" y2="250" stroke="#c1c7d0" stroke-width="1.2"/>
    <line x1="960" y1="20" x2="960" y2="250" stroke="#c1c7d0" stroke-width="1.2"/>
    <line x1="40" y1="250" x2="960" y2="250" stroke="#c1c7d0" stroke-width="1.2"/>
    <line x1="40" y1="20" x2="960" y2="20" stroke="#c1c7d0" stroke-width="1.2"/>

    <!-- X-axis Ticks & Dates (11 intervals from 05 oct to 26 oct) -->
    <text x="40" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">05 oct</text>
    <text x="132" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">07 oct</text>
    <text x="224" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">09 oct</text>
    <text x="316" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">11 oct</text>
    <text x="408" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">13 oct</text>
    <text x="500" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">15 oct</text>
    <text x="592" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">17 oct</text>
    <text x="684" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">19 oct</text>
    <text x="776" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">21 oct</text>
    <text x="868" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">23 oct</text>
    <text x="960" y="268" font-size="10.5" fill="#6b778c" text-anchor="middle">26 oct</text>

    <!-- 1. Línea de Guía (Ideal diagonal from (40, 20) to (960, 250)) -->
    <line x1="40" y1="20" x2="960" y2="250" stroke="#8993a4" stroke-width="1.8"/>

    <!-- 2. Trabajo restante (Red staircase line descending to 0 on 26 oct) -->
    <!-- 05 oct - 11 oct: 6 (Y=20) -->
    <!-- 11 oct: step down to 5 (Y=58.33) -->
    <!-- 14 oct: step down to 4 (Y=96.66) -->
    <!-- 17 oct: step down to 3 (Y=135) -->
    <!-- 20 oct: step down to 2 (Y=173.33) -->
    <!-- 23 oct: step down to 1 (Y=211.66) -->
    <!-- 26 oct: step down to 0 (Y=250) -->
    <path d="M 40 20
             L 316 20
             L 316 58.33
             L 454 58.33
             L 454 96.66
             L 592 96.66
             L 592 135
             L 730 135
             L 730 173.33
             L 868 173.33
             L 868 211.66
             L 955 211.66
             L 955 250
             L 960 250"
          fill="none" stroke="#de350b" stroke-width="2.6" stroke-linejoin="miter"/>
  </svg>
</div>

</body>
</html>
"""

# Guardar ambos archivos HTML
html_burnup_file = os.path.abspath(r"documentacion\jira_burnup_sprint3.html")
html_burndown_file = os.path.abspath(r"documentacion\jira_burndown_sprint3.html")

with open(html_burnup_file, "w", encoding="utf-8") as f:
    f.write(burnup_html)

with open(html_burndown_file, "w", encoding="utf-8") as f:
    f.write(burndown_html)

print("Archivos HTML creados exitosamente.")

# Renderizar Imagen 1: Burnup Report
out_burnup_local = os.path.join(OUTPUT_DIR, "jira_burnup_report_sprint3.png")
cmd1 = [
    CHROME_PATH,
    "--headless",
    "--disable-gpu",
    "--force-device-scale-factor=2",
    "--window-size=1060,560",
    f"--screenshot={out_burnup_local}",
    f"file:///{html_burnup_file.replace(os.sep, '/')}"
]
print("Renderizando Burnup Report con Chrome...")
subprocess.run(cmd1, check=True)

# Renderizar Imagen 2: Burndown Report
out_burndown_local = os.path.join(OUTPUT_DIR, "jira_burndown_report_sprint3.png")
cmd2 = [
    CHROME_PATH,
    "--headless",
    "--disable-gpu",
    "--force-device-scale-factor=2",
    "--window-size=1060,560",
    f"--screenshot={out_burndown_local}",
    f"file:///{html_burndown_file.replace(os.sep, '/')}"
]
print("Renderizando Burndown Report con Chrome...")
subprocess.run(cmd2, check=True)

# Copiar a assets y downloads
for src, name in [
    (out_burnup_local, "jira_burnup_report_sprint3.png"),
    (out_burndown_local, "jira_burndown_report_sprint3.png")
]:
    shutil.copy2(src, os.path.join(ASSETS_DIR, name))
    shutil.copy2(src, os.path.join(DOWNLOADS_DIR, name))
    print(f"Exportado: {name} a {ASSETS_DIR} y {DOWNLOADS_DIR}")

print("\n¡Ambos reportes (Burnup y Burndown) fueron generados con exito!")
