import { Injectable, signal, NgZone } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ReconocimientoVozService {
  private recognition: any = null;
  public escuchando = signal<boolean>(false);
  public textoDetectado = signal<string>('');
  public soportado = signal<boolean>(false);
  public error = signal<string | null>(null);

  // Callback opcional cuando finaliza el dictado
  private onFinalCallback?: (textoFinal: string) => void;

  constructor(private readonly zone: NgZone) {
    this.inicializar();
  }

  private inicializar() {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (SpeechRecognition) {
      this.soportado.set(true);
      try {
        this.recognition = new SpeechRecognition();
        this.recognition.lang = 'es-ES';
        this.recognition.continuous = false;
        this.recognition.interimResults = true;
        this.recognition.maxAlternatives = 1;

        this.recognition.onstart = () => {
          this.zone.run(() => {
            this.escuchando.set(true);
            this.error.set(null);
          });
        };

        this.recognition.onresult = (event: any) => {
          this.zone.run(() => {
            let transcripcion = '';
            for (let i = 0; i < event.results.length; i++) {
              transcripcion += event.results[i][0].transcript;
            }
            this.textoDetectado.set(transcripcion);
          });
        };

        this.recognition.onerror = (event: any) => {
          this.zone.run(() => {
            this.escuchando.set(false);
            if (event.error === 'not-allowed') {
              this.error.set('Permiso de micrófono denegado. Permite el acceso en el navegador.');
            } else if (event.error === 'no-speech') {
              this.error.set('No se detectó voz. Vuelve a intentar hablando cerca al micrófono.');
            } else {
              this.error.set(`Error de micrófono: ${event.error || 'desconocido'}`);
            }
          });
        };

        this.recognition.onend = () => {
          this.zone.run(() => {
            this.escuchando.set(false);
            const resultado = this.textoDetectado().trim();
            if (resultado && this.onFinalCallback) {
              const cb = this.onFinalCallback;
              this.onFinalCallback = undefined;
              cb(resultado);
            }
          });
        };
      } catch (err) {
        this.soportado.set(false);
        this.error.set('No se pudo inicializar la API de reconocimiento de voz.');
      }
    } else {
      this.soportado.set(false);
      this.error.set('Tu navegador no soporta reconocimiento de voz nativo (Web Speech API). Usa Chrome o Edge.');
    }
  }

  public iniciar(onFinal?: (texto: string) => void) {
    if (!this.recognition) {
      this.error.set('El navegador no soporta reconocimiento de voz. Usa Chrome o Edge.');
      return;
    }
    this.onFinalCallback = onFinal;
    this.error.set(null);
    this.textoDetectado.set('');
    try {
      this.recognition.start();
    } catch (e: any) {
      try {
        this.recognition.abort();
        setTimeout(() => {
          try {
            this.recognition.start();
          } catch (err) {}
        }, 150);
      } catch (err) {}
    }
  }

  public detener() {
    if (!this.recognition) return;
    try {
      this.recognition.stop();
    } catch (e) {
      this.zone.run(() => this.escuchando.set(false));
    }
  }
}

