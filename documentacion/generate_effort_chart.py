# -*- coding: utf-8 -*-
"""
Genera la imagen exacta de la 'Grafica de esfuerzos y datos de esfuerzo' para el Sprint 3:
- Formato idéntico al Sprint 2 (media_1793031870744_bda893b9.png).
- Título superior: '7. Grafica de esfuerzos y datos de esfuerzo'
- Título del gráfico: 'Comparación de esfuerzo planificado y real por integrante - Sprint 3'
- Barras agrupadas por integrante: Planificado vs Real con los colores exactos de cada integrante.
- Valores basados en el Sprint Backlog del Sprint 3 (Total planificado: 94 hr).
- Leyenda inferior redondeada: 'Planificado' / 'Real'.
"""

import os
import shutil
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch
import numpy as np

OUTPUT_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\diagramas"
ASSETS_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\sprint3_assets"
DOWNLOADS_DIR = r"C:\Users\ASUS\Downloads"
os.makedirs(OUTPUT_DIR, exist_ok=True)
os.makedirs(ASSETS_DIR, exist_ok=True)
os.makedirs(DOWNLOADS_DIR, exist_ok=True)

PNG_NAME = "grafica_esfuerzos_sprint3.png"
OUT_LOCAL = os.path.join(OUTPUT_DIR, PNG_NAME)
OUT_ASSET = os.path.join(ASSETS_DIR, PNG_NAME)
OUT_DOWNLOADS = os.path.join(DOWNLOADS_DIR, PNG_NAME)

# Datos del Sprint Backlog Sprint 3
integrantes = ['Roberto', 'Matteo', 'Kevin', 'Carlos', 'Cristian', 'Raúl']
planificado = [15.0, 16.0, 18.0, 18.0, 15.0, 12.0]
real =        [16.5, 17.5, 19.0, 19.0, 16.0, 13.0]

# Colores exactos del Sprint 2 por integrante: (Planificado / Lighter, Real / Darker)
colors_plan = ['#64b5f6', '#81c784', '#e57373', '#ba68c8', '#ffb74d', '#4dd0e1']
colors_real = ['#1976d2', '#2e7d32', '#c62828', '#6a1b9a', '#e65100', '#00838f']

# Crear figura con proporción y resolución idéntica a la muestra
fig = plt.figure(figsize=(9.2, 6.2), dpi=200, facecolor='#ffffff')

# Título de sección de documento arriba a la izquierda
fig.text(0.08, 0.94, '7. Grafica de esfuerzos y datos de esfuerzo', 
         fontsize=16, fontweight='bold', color='#154360', fontfamily='sans-serif')

# Título del gráfico centrado arriba del plot
fig.text(0.50, 0.85, 'Comparación de esfuerzo planificado y real por integrante - Sprint 3', 
         fontsize=13, fontweight='normal', color='#000000', ha='center', fontfamily='sans-serif')

# Ejes
ax = fig.add_axes([0.15, 0.20, 0.76, 0.60])

x = np.arange(len(integrantes))
width = 0.38

# Barras
bars_plan = []
bars_real = []

for i in range(len(integrantes)):
    b_p = ax.bar(x[i] - width/2, planificado[i], width, color=colors_plan[i], edgecolor='none')
    b_r = ax.bar(x[i] + width/2, real[i], width, color=colors_real[i], edgecolor='none')
    bars_plan.append(b_p)
    bars_real.append(b_r)

# Anotaciones de valor numérico arriba de cada barra
for i in range(len(integrantes)):
    # Planificado
    val_p = planificado[i]
    str_p = f"{int(val_p)}" if val_p.is_integer() else f"{val_p}"
    ax.annotate(str_p,
                xy=(x[i] - width/2, val_p),
                xytext=(0, 4), textcoords="offset points",
                ha='center', va='bottom', fontsize=11, fontweight='normal', color='#000000')
    
    # Real
    val_r = real[i]
    str_r = f"{int(val_r)}" if val_r.is_integer() else f"{val_r}"
    ax.annotate(str_r,
                xy=(x[i] + width/2, val_r),
                xytext=(0, 4), textcoords="offset points",
                ha='center', va='bottom', fontsize=11, fontweight='normal', color='#000000')

# Configuración de ejes
ax.set_ylabel('Horas', fontsize=13, fontfamily='sans-serif', color='#000000')
ax.set_xticks(x)
ax.set_xticklabels(integrantes, fontsize=12, fontfamily='sans-serif', color='#000000')
ax.set_ylim(0, 30.0)
ax.set_yticks([0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0])
ax.set_yticklabels(['0.0', '5.0', '10.0', '15.0', '20.0', '25.0', '30.0'], fontsize=11, fontfamily='sans-serif')

# Bordes estándar
ax.spines['top'].set_visible(True)
ax.spines['right'].set_visible(True)
ax.spines['left'].set_visible(True)
ax.spines['bottom'].set_visible(True)
ax.spines['top'].set_color('#000000')
ax.spines['right'].set_color('#000000')
ax.spines['left'].set_color('#000000')
ax.spines['bottom'].set_color('#000000')

# Leyenda personalizada centrada abajo con caja redondeada
# Swatches gris claro y azul pizarra oscuro
patch_plan = mpatches.Patch(color='#b0bec5', label='Planificado')
patch_real = mpatches.Patch(color='#37474f', label='Real')

leg = ax.legend(handles=[patch_plan, patch_real], loc='upper center', 
                bbox_to_anchor=(0.5, -0.14), ncol=2, 
                frameon=True, fontsize=11, handlelength=1.4, handleheight=1.0)

leg.get_frame().set_edgecolor('#90a4ae')
leg.get_frame().set_boxstyle('round,pad=0.6,rounding_size=0.8')
leg.get_frame().set_linewidth(1.0)
leg.get_frame().set_facecolor('#ffffff')

# Guardar figura
plt.savefig(OUT_LOCAL, facecolor=fig.get_facecolor(), edgecolor='none', bbox_inches='tight')
plt.close(fig)

print(f"Grafica de esfuerzos guardada en: {OUT_LOCAL}")

# Copiar a assets y descargas
shutil.copy2(OUT_LOCAL, OUT_ASSET)
shutil.copy2(OUT_LOCAL, OUT_DOWNLOADS)
print(f"Copiado a: {OUT_ASSET}")
print(f"Copiado a: {OUT_DOWNLOADS}")
