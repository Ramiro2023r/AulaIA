import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { EstudianteDetalleComponent } from './estudiante-detalle.component';
import { EstudianteService } from '../../../../../core/services/estudiante.service';

describe('EstudianteDetalleComponent - vinculación Telegram', () => {
  let component: EstudianteDetalleComponent;
  let estudianteService: jasmine.SpyObj<EstudianteService>;

  beforeEach(async () => {
    estudianteService = jasmine.createSpyObj<EstudianteService>('EstudianteService', [
      'buscarPorId', 'regenerarQr', 'generarVinculacionTelegram', 'listarApoderadosParaTelegram', 'crearApoderado'
    ]);
    estudianteService.generarVinculacionTelegram.and.returnValue(
      throwError(() => ({ status: 400, error: { code: 'TELEGRAM_APODERADO_REQUIRED' } }))
    );

    await TestBed.configureTestingModule({
      imports: [EstudianteDetalleComponent],
      providers: [
        { provide: EstudianteService, useValue: estudianteService },
        { provide: HttpClient, useValue: jasmine.createSpyObj<HttpClient>('HttpClient', ['get']) },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } }
      ]
    }).compileComponents();

    component = TestBed.createComponent(EstudianteDetalleComponent).componentInstance;
    component.estudiante.set({
      id: 7, codigo: 'A-007', nombres: 'Diego', apellidos: 'Pérez',
      seccion: { id: 1, nombre: 'A' }, activo: true, createdAt: '', updatedAt: ''
    });
  });

  it('envía el apoderado seleccionado al generar la vinculación', () => {
    component.apoderadosTelegram.set([
      { id: 12, nombres: 'María', apellidos: 'Pérez', parentesco: 'MADRE', principal: true, activo: true },
      { id: 13, nombres: 'Carlos', apellidos: 'Pérez', parentesco: 'PADRE', principal: false, activo: true }
    ]);
    component.seleccionarApoderado({ target: { value: '13' } } as unknown as Event);

    component.generarQrTelegram();

    expect(estudianteService.generarVinculacionTelegram).toHaveBeenCalledWith(7, 13);
  });

  it('no permite generar el QR cuando no hay apoderados', () => {
    component.apoderadosTelegram.set([]);
    component.apoderadoSeleccionadoId.set(null);

    component.generarQrTelegram();

    expect(estudianteService.generarVinculacionTelegram).not.toHaveBeenCalled();
    expect(component.telegramError()).toBe('No hay apoderados registrados para este estudiante.');
  });

  it('registra y asocia un apoderado al estudiante', () => {
    estudianteService.crearApoderado.and.returnValue(of({
      id: 15, nombres: 'María', apellidos: 'Pérez', parentesco: 'MADRE', principal: true, activo: true
    }));
    component.nuevoApoderado = {
      nombres: 'María', apellidos: 'Pérez', telefono: '999111222', parentesco: 'MADRE', principal: true
    };

    component.guardarApoderado();

    expect(estudianteService.crearApoderado).toHaveBeenCalledWith(7, {
      nombres: 'María', apellidos: 'Pérez', telefono: '999111222', parentesco: 'MADRE', principal: true
    });
    expect(component.apoderadosTelegram()[0].id).toBe(15);
    expect(component.apoderadoExito()).toBe('Apoderado registrado y asociado correctamente.');
  });
});
