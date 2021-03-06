import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {MfaComponent} from './mfa.component';
import { GroovyComponent } from './groovy/groovy.component';
import {SharedModule} from '../../shared/shared.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { DefaultComponent } from './default/default.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    FormsModule
  ],
  declarations: [
    MfaComponent,
    GroovyComponent,
    DefaultComponent
  ],
  exports: [
    MfaComponent
  ]
})
export class MfaModule { }
