import { DestroyRef, Directive, ElementRef, effect, Input, OnChanges, Renderer2 } from '@angular/core';
import { UiComponentKey, UiPrivilegeState } from './ui-privileges.model';
import { UiPrivilegesService } from './ui-privileges.service';

@Directive({
  selector: '[appUiPrivilegeVisible]',
  standalone: true,
})
export class UiPrivilegeVisibleDirective implements OnChanges {
  @Input({ alias: 'appUiPrivilegeVisible', required: true }) key!: UiComponentKey;

  constructor(
    private readonly elementRef: ElementRef<HTMLElement>,
    private readonly renderer: Renderer2,
    private readonly privileges: UiPrivilegesService,
    destroyRef: DestroyRef,
  ) {
    const policyEffect = effect(() => this.applyPrivilegeState(), { manualCleanup: true });
    destroyRef.onDestroy(() => policyEffect.destroy());
  }

  ngOnChanges(): void {
    this.applyPrivilegeState();
  }

  private applyPrivilegeState(): void {
    if (!this.key) return;

    const allowed = this.allows('VISIBLE');
    this.setHidden(!allowed);
  }

  private allows(state: UiPrivilegeState): boolean {
    return this.privileges.allows(this.key, state);
  }

  private setHidden(hidden: boolean): void {
    const element = this.elementRef.nativeElement;
    this.renderer.setProperty(element, 'hidden', hidden);

    if (hidden) {
      this.renderer.setAttribute(element, 'hidden', '');
    } else {
      this.renderer.removeAttribute(element, 'hidden');
    }
  }
}

export interface UiPrivilegeEnabledConfig {
  key: UiComponentKey;
  businessDisabled?: boolean;
}

type UiPrivilegeEnabledInput = UiComponentKey | UiPrivilegeEnabledConfig;

@Directive({
  selector: '[appUiPrivilegeEnabled]',
  standalone: true,
})
export class UiPrivilegeEnabledDirective implements OnChanges {
  @Input({ alias: 'appUiPrivilegeEnabled', required: true }) privilege!: UiPrivilegeEnabledInput;

  constructor(
    private readonly elementRef: ElementRef<HTMLElement>,
    private readonly renderer: Renderer2,
    private readonly privileges: UiPrivilegesService,
    destroyRef: DestroyRef,
  ) {
    const policyEffect = effect(() => this.applyPrivilegeState(), { manualCleanup: true });
    destroyRef.onDestroy(() => policyEffect.destroy());
  }

  ngOnChanges(): void {
    this.applyPrivilegeState();
  }

  private applyPrivilegeState(): void {
    const config = this.resolveConfig();
    if (!config.key) return;

    const visible = this.privileges.allows(config.key, 'VISIBLE');
    const allowed = visible && this.privileges.allows(config.key, 'HABILITADO');
    setHiddenState(this.elementRef.nativeElement, this.renderer, !visible);
    setDisabledState(this.elementRef.nativeElement, this.renderer, !allowed || config.businessDisabled);
  }

  private resolveConfig(): { key?: UiComponentKey; businessDisabled: boolean } {
    if (typeof this.privilege === 'string') {
      return { key: this.privilege, businessDisabled: false };
    }

    return {
      key: this.privilege?.key,
      businessDisabled: Boolean(this.privilege?.businessDisabled),
    };
  }
}

export interface UiPrivilegeEditableConfig {
  key: UiComponentKey;
  businessReadOnly?: boolean;
}

type UiPrivilegeEditableInput = UiComponentKey | UiPrivilegeEditableConfig;

@Directive({
  selector: '[appUiPrivilegeEditable]',
  standalone: true,
})
export class UiPrivilegeEditableDirective implements OnChanges {
  @Input({ alias: 'appUiPrivilegeEditable', required: true }) privilege!: UiPrivilegeEditableInput;

  constructor(
    private readonly elementRef: ElementRef<HTMLElement>,
    private readonly renderer: Renderer2,
    private readonly privileges: UiPrivilegesService,
    destroyRef: DestroyRef,
  ) {
    const policyEffect = effect(() => this.applyPrivilegeState(), { manualCleanup: true });
    destroyRef.onDestroy(() => policyEffect.destroy());
  }

  ngOnChanges(): void {
    this.applyPrivilegeState();
  }

  private applyPrivilegeState(): void {
    const config = this.resolveConfig();
    if (!config.key) return;

    const visible = this.privileges.allows(config.key, 'VISIBLE');
    const allowed = visible && this.privileges.allows(config.key, 'EDITABLE');
    setHiddenState(this.elementRef.nativeElement, this.renderer, !visible);
    setEditableState(this.elementRef.nativeElement, this.renderer, allowed && !config.businessReadOnly);
  }

  private resolveConfig(): { key?: UiComponentKey; businessReadOnly: boolean } {
    if (typeof this.privilege === 'string') {
      return { key: this.privilege, businessReadOnly: false };
    }

    return {
      key: this.privilege?.key,
      businessReadOnly: Boolean(this.privilege?.businessReadOnly),
    };
  }
}

function setHiddenState(element: HTMLElement, renderer: Renderer2, hidden: boolean): void {
  renderer.setProperty(element, 'hidden', hidden);

  if (hidden) {
    renderer.setAttribute(element, 'hidden', '');
  } else {
    renderer.removeAttribute(element, 'hidden');
  }
}

function setEditableState(element: HTMLElement, renderer: Renderer2, editable: boolean): void {
  if (supportsReadOnly(element)) {
    renderer.setProperty(element, 'disabled', false);
    renderer.removeAttribute(element, 'disabled');
    setReadOnlyState(element, renderer, !editable);
    return;
  }

  setDisabledState(element, renderer, !editable);
}

function supportsReadOnly(element: HTMLElement): element is HTMLInputElement | HTMLTextAreaElement {
  if (element instanceof HTMLTextAreaElement) return true;
  if (!(element instanceof HTMLInputElement)) return false;

  return !['button', 'checkbox', 'color', 'file', 'hidden', 'image', 'radio', 'range', 'reset', 'submit'].includes(
    element.type,
  );
}

function setReadOnlyState(
  element: HTMLInputElement | HTMLTextAreaElement,
  renderer: Renderer2,
  readOnly: boolean,
): void {
  renderer.setProperty(element, 'readOnly', readOnly);

  if (readOnly) {
    renderer.setAttribute(element, 'readonly', '');
    renderer.setAttribute(element, 'aria-disabled', 'true');
  } else {
    renderer.removeAttribute(element, 'readonly');
    renderer.removeAttribute(element, 'aria-disabled');
  }
}

function setDisabledState(element: HTMLElement, renderer: Renderer2, disabled: boolean): void {
  renderer.setProperty(element, 'disabled', disabled);

  if (disabled) {
    renderer.setAttribute(element, 'disabled', '');
    renderer.setAttribute(element, 'aria-disabled', 'true');
  } else {
    renderer.removeAttribute(element, 'disabled');
    renderer.removeAttribute(element, 'aria-disabled');
  }
}
