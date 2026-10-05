import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiPrivilegeEditableDirective, UiPrivilegeEnabledDirective, UiPrivilegeVisibleDirective } from './ui-privilege.directive';
import { UiPrivilegesService } from './ui-privileges.service';

@Component({
  standalone: true,
  imports: [UiPrivilegeVisibleDirective, UiPrivilegeEnabledDirective, UiPrivilegeEditableDirective],
  template: `
    <section id="visible" [appUiPrivilegeVisible]="visibleKey">Visible content</section>
    <button id="enabled" type="button" [appUiPrivilegeEnabled]="{ key: enabledKey, businessDisabled: enabledBusinessDisabled() }">Run</button>
    <input id="editableInput" [appUiPrivilegeEditable]="editableInputKey" />
    <input id="businessReadonlyInput" [appUiPrivilegeEditable]="{ key: editableInputKey, businessReadOnly: editableBusinessReadOnly() }" />
    <select id="editableSelect" [appUiPrivilegeEditable]="editableSelectKey">
      <option>One</option>
    </select>
  `,
})
class HostComponent {
  visibleKey = 'panel.summary';
  enabledKey = 'events.create.button';
  enabledBusinessDisabled = signal(false);
  editableBusinessReadOnly = signal(false);
  editableInputKey = 'events.name.field';
  editableSelectKey = 'events.category.select';
}

describe('UI privilege directives', () => {
  let service: jasmine.SpyObj<UiPrivilegesService>;
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(() => {
    service = jasmine.createSpyObj<UiPrivilegesService>('UiPrivilegesService', ['allows']);

    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [{ provide: UiPrivilegesService, useValue: service }],
    });
  });

  it('hides a host when its VISIBLE privilege is denied', () => {
    service.allows.and.returnValue(false);

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement.querySelector('#visible') as HTMLElement;
    expect(service.allows).toHaveBeenCalledWith('panel.summary', 'VISIBLE');
    expect(element.hidden).toBeTrue();
  });

  it('leaves a host visible when its VISIBLE privilege is granted', () => {
    service.allows.and.callFake((key, state) => key === 'panel.summary' && state === 'VISIBLE');

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement.querySelector('#visible') as HTMLElement;
    expect(element.hidden).toBeFalse();
  });

  it('disables an interactive host and exposes aria-disabled when HABILITADO is denied', () => {
    service.allows.and.callFake((key, state) => key === 'events.create.button' && state === 'VISIBLE');

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('#enabled') as HTMLButtonElement;
    expect(service.allows).toHaveBeenCalledWith('events.create.button', 'HABILITADO');
    expect(button.disabled).toBeTrue();
    expect(button.getAttribute('aria-disabled')).toBe('true');
  });

  it('keeps an interactive host disabled when HABILITADO is granted but business state disables it', () => {
    service.allows.and.callFake((key, state) => key === 'events.create.button' && state === 'HABILITADO');

    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.enabledBusinessDisabled.set(true);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('#enabled') as HTMLButtonElement;
    expect(button.disabled).toBeTrue();
    expect(button.getAttribute('aria-disabled')).toBe('true');
  });

  it('disables an interactive host when HABILITADO is denied even if business state allows it', () => {
    service.allows.and.returnValue(false);

    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.enabledBusinessDisabled.set(false);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('#enabled') as HTMLButtonElement;
    expect(button.disabled).toBeTrue();
    expect(button.getAttribute('aria-disabled')).toBe('true');
  });

  it('hides and disables an enabled host when HABILITADO is granted without VISIBLE', () => {
    service.allows.and.callFake((key, state) => key === 'events.create.button' && state === 'HABILITADO');

    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.enabledBusinessDisabled.set(false);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('#enabled') as HTMLButtonElement;
    expect(service.allows).toHaveBeenCalledWith('events.create.button', 'VISIBLE');
    expect(button.hidden).toBeTrue();
    expect(button.disabled).toBeTrue();
    expect(button.getAttribute('aria-disabled')).toBe('true');
  });

  it('enables an interactive host only when VISIBLE and HABILITADO are granted and business state allows it', () => {
    service.allows.and.callFake(
      (key, state) => key === 'events.create.button' && (state === 'VISIBLE' || state === 'HABILITADO'),
    );

    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.enabledBusinessDisabled.set(false);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('#enabled') as HTMLButtonElement;
    expect(button.hidden).toBeFalse();
    expect(button.disabled).toBeFalse();
    expect(button.hasAttribute('aria-disabled')).toBeFalse();
  });

  it('makes text controls read-only when EDITABLE is denied', () => {
    service.allows.and.callFake((key, state) => key === 'events.name.field' && state === 'VISIBLE');

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#editableInput') as HTMLInputElement;
    expect(service.allows).toHaveBeenCalledWith('events.name.field', 'EDITABLE');
    expect(input.readOnly).toBeTrue();
    expect(input.disabled).toBeFalse();
    expect(input.getAttribute('aria-disabled')).toBe('true');
  });

  it('keeps a text control read-only when EDITABLE is granted but business state is read-only', () => {
    service.allows.and.callFake((key, state) => key === 'events.name.field' && state === 'EDITABLE');

    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.editableBusinessReadOnly.set(true);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#businessReadonlyInput') as HTMLInputElement;
    expect(input.readOnly).toBeTrue();
    expect(input.disabled).toBeFalse();
    expect(input.getAttribute('aria-disabled')).toBe('true');
  });

  it('hides and keeps editable text read-only when EDITABLE is granted without VISIBLE', () => {
    service.allows.and.callFake((key, state) => key === 'events.name.field' && state === 'EDITABLE');

    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.editableBusinessReadOnly.set(false);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#businessReadonlyInput') as HTMLInputElement;
    expect(service.allows).toHaveBeenCalledWith('events.name.field', 'VISIBLE');
    expect(input.hidden).toBeTrue();
    expect(input.readOnly).toBeTrue();
    expect(input.getAttribute('aria-disabled')).toBe('true');
  });

  it('makes a text control editable only when VISIBLE and EDITABLE are granted and business state allows edits', () => {
    service.allows.and.callFake(
      (key, state) => key === 'events.name.field' && (state === 'VISIBLE' || state === 'EDITABLE'),
    );

    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.editableBusinessReadOnly.set(false);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#businessReadonlyInput') as HTMLInputElement;
    expect(input.hidden).toBeFalse();
    expect(input.readOnly).toBeFalse();
    expect(input.hasAttribute('aria-disabled')).toBeFalse();
  });

  it('safely disables non-readonly editable controls when EDITABLE is denied', () => {
    service.allows.and.callFake((key, state) => key === 'events.category.select' && state === 'VISIBLE');

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('#editableSelect') as HTMLSelectElement;
    expect(service.allows).toHaveBeenCalledWith('events.category.select', 'EDITABLE');
    expect(select.disabled).toBeTrue();
    expect(select.getAttribute('aria-disabled')).toBe('true');
  });

  it('updates rendered state when the loaded policy changes after initial render', () => {
    const grants = signal(new Set<string>());
    service.allows.and.callFake((key, state) => grants().has(`${key}:${state}`));

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const visible = fixture.nativeElement.querySelector('#visible') as HTMLElement;
    const button = fixture.nativeElement.querySelector('#enabled') as HTMLButtonElement;
    const input = fixture.nativeElement.querySelector('#editableInput') as HTMLInputElement;

    expect(visible.hidden).toBeTrue();
    expect(button.disabled).toBeTrue();
    expect(input.readOnly).toBeTrue();

    fixture.componentInstance.enabledBusinessDisabled.set(true);
    grants.set(
      new Set([
        'panel.summary:VISIBLE',
        'events.create.button:VISIBLE',
        'events.create.button:HABILITADO',
        'events.name.field:VISIBLE',
        'events.name.field:EDITABLE',
      ]),
    );
    fixture.detectChanges();

    expect(visible.hidden).toBeFalse();
    expect(button.disabled).toBeTrue();
    expect(button.getAttribute('aria-disabled')).toBe('true');

    fixture.componentInstance.enabledBusinessDisabled.set(false);
    fixture.detectChanges();

    expect(button.disabled).toBeFalse();
    expect(button.hasAttribute('aria-disabled')).toBeFalse();
    expect(input.readOnly).toBeFalse();
    expect(input.hasAttribute('aria-disabled')).toBeFalse();
  });
});
