package com.mmd.dieta;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;

final class FitnessUI {
    final MainActivity a;
    final SharedPreferences prefs;
    final Handler handler=new Handler(Looper.getMainLooper());
    int week=1,day=0,phase=0,group=0,exercise=0,set=0;
    List<FitnessData.Step> steps;
    int index=0; long end=0,remaining=0; boolean running=false,resting=false;
    int lastAlert=-1;
    String mode="";
    TextView timer,stage,next,detail; Button play,advance;
    final Runnable tick=new Runnable(){public void run(){refresh();if(running)handler.postDelayed(this,100);}};
    FitnessUI(MainActivity a){this.a=a;prefs=PlanData.prefs(a);week=prefs.getInt("run_week",1);day=prefs.getInt("run_day",0);phase=prefs.getInt("gym_phase",0);group=prefs.getInt("gym_group",0);}
    int dp(int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
    TextView txt(String s,int size,boolean bold){TextView t=new TextView(a);t.setText(s);t.setTextColor(0xffecf7ff);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    void line(LinearLayout c,String s){TextView t=txt(s,14,false);t.setTextColor(0xffa8bad2);c.addView(t);t.setPadding(0,dp(5),0,dp(5));}
    Button btn(LinearLayout c,String s,Runnable f){Button b=new Button(a);b.setAllCaps(false);b.setText(s);b.setTextColor(0xffeaf7ff);GradientDrawable d=new GradientDrawable();d.setColor(0xff1a3452);d.setCornerRadius(dp(14));b.setBackground(d);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.topMargin=dp(7);c.addView(b,p);b.setOnClickListener(v->f.run());return b;}
    void choose(String title,String[] labels,int value,java.util.function.IntConsumer f){new AlertDialog.Builder(a).setTitle(title).setSingleChoiceItems(labels,value,(d,i)->{d.dismiss();f.accept(i);}).setNegativeButton("Cancelar",null).show();}
    boolean busy(){return !mode.isEmpty();}
    void wake(boolean b){if(b)a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    void buzz(long ms){Vibrator v=(Vibrator)a.getSystemService(Context.VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(ms,VibrationEffect.DEFAULT_AMPLITUDE));}
    String time(long ms){long s=Math.max(0,(ms+999)/1000);return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
    void runningScreen(LinearLayout c){
        if(mode.equals("run")){session(c);return;}
        line(c,"12 semanas • 3 sessões por semana • ritmo de conversa");
        btn(c,"Semana "+week+"  •  Fase "+((week-1)/4+1),()->choose("Semana de corrida",new String[]{"1","2","3","4","5","6","7","8","9","10","11","12"},week-1,i->{week=i+1;prefs.edit().putInt("run_week",week).apply();a.render();}));
        String[] days={"Segunda-feira","Quarta-feira","Sábado"};
        btn(c,days[day],()->choose("Dia de corrida",days,day,i->{day=i;prefs.edit().putInt("run_day",day).apply();a.render();}));
        List<FitnessData.Step> plan=FitnessData.run(week,day);
        int total=0,blocks=0;for(FitnessData.Step s:plan){total+=s.seconds;if(s.name.startsWith("RUNL"))blocks++;}
        line(c,"Tempo programado: "+total/60+" min • "+blocks+" blocos de trote. Desafios de distância têm confirmação manual.");
        if(week>=9)line(c,"Fase 3 sugerida, não faz parte da ficha original. Só avance sem dor e após concluir 3 km confortavelmente; repita semanas se necessário.");
        line(c,"Mantenha esta tela aberta durante o treino. Não depende de internet. Sem GPS nesta versão.");
        btn(c,"▶ Iniciar corrida",()->new AlertDialog.Builder(a).setTitle("Preparar corrida").setMessage("Treine em ritmo confortável. Pare com dor, tontura, falta de ar fora do habitual ou desconforto no peito. Mantenha a tela aberta.").setNegativeButton("Cancelar",null).setPositiveButton("Iniciar",(d,w)->{steps=plan;index=0;mode="run";remaining=steps.get(0).seconds*1000L;startClock();a.render();}).show());
        btn(c,"Ver etapas completas",()->{StringBuilder s=new StringBuilder();for(FitnessData.Step step:plan)s.append(step.name).append("  •  ").append(step.seconds==0?"manual":step.seconds/60+" min").append("\n");new AlertDialog.Builder(a).setTitle("Sequência do dia").setMessage(s.toString()).setPositiveButton("Fechar",null).show();});
        line(c,prefs.getBoolean("run_done_"+week+"_"+day,false)?"✓ Sessão registrada":"Sessão ainda não concluída.");
    }
    void gymScreen(LinearLayout c){
        if(mode.equals("gym")){session(c);return;}
        String[] names={"A • Peito e bíceps","B • Ombros e trapézio","C • Costas e tríceps","D • Pernas"};
        String[] phases={"Semanas 1–2","Semanas 3–4","Semanas 5–6"};
        btn(c,names[group],()->choose("Ficha",names,group,i->{group=i;prefs.edit().putInt("gym_group",i).apply();a.render();}));
        btn(c,phases[phase],()->choose("Período",phases,phase,i->{phase=i;prefs.edit().putInt("gym_phase",i).apply();a.render();}));
        line(c,"Fichas originais de 2017. Retome com cargas leves e séries reduzidas se necessário; não force falha muscular. A cadência é interpretada como segundos de subida:descida.");
        btn(c,"▶ Iniciar musculação",()->{exercise=0;set=0;resting=false;mode="gym";remaining=0;running=false;a.render();});
        btn(c,"Ver ficha completa",()->{StringBuilder s=new StringBuilder();for(FitnessData.Exercise e:FitnessData.GYM[group])s.append(e.name).append("\n").append(e.tech).append(" • ").append(e.sets).append(" séries • ").append(e.reps[phase]).append(" reps • ").append(e.tempo[phase]).append(" • ").append(e.rest[phase]).append("s descanso\n\n");new AlertDialog.Builder(a).setTitle(names[group]).setMessage(s.toString()).setPositiveButton("Fechar",null).show();});
    }
    void startClock(){if(remaining<=0)return;running=true;end=SystemClock.elapsedRealtime()+remaining;lastAlert=-1;wake(true);handler.removeCallbacks(tick);handler.post(tick);}
    void pause(){if(running)remaining=Math.max(0,end-SystemClock.elapsedRealtime());running=false;handler.removeCallbacks(tick);refresh();}
    void session(LinearLayout c){
        stage=txt("",23,true);c.addView(stage);
        detail=txt("",15,false);c.addView(detail);
        timer=txt("",66,true);timer.setGravity(Gravity.CENTER);c.addView(timer,new LinearLayout.LayoutParams(-1,dp(115)));
        next=txt("",15,true);next.setGravity(Gravity.CENTER);c.addView(next);
        play=btn(c,"",()->{if(running)pause();else if(remaining>0)startClock();else if(mode.equals("gym")&&!resting){remaining=estimatedSeconds()*1000L;startClock();}refresh();});
        advance=btn(c,"",()->{if(mode.equals("run")){if(!running&&remaining==0)nextRun();else new AlertDialog.Builder(a).setTitle("Pular etapa?").setNegativeButton("Cancelar",null).setPositiveButton("Pular",(d,w)->nextRun()).show();}else if(resting)nextSet();else recordSet();});
        if(mode.equals("gym"))btn(c,"Técnica e cadência",()->{FitnessData.Exercise e=current();String help=e.tech.equals("Pirâmide")?"Repetições por série conforme a ficha; ajuste carga com técnica.":e.tech.equals("Drop set")?"Reduza a carga após o bloco inicial. Não é necessário ir à falha ao retomar.":e.tech.equals("Bi-set")||e.tech.equals("Super set")?"Faça os dois movimentos em sequência, depois descanse.":"Repita as séries previstas com descanso entre elas.";new AlertDialog.Builder(a).setTitle(e.tech).setMessage(help+"\nRitmo "+e.tempo[phase]+": interpretação subida:descida.").setPositiveButton("OK",null).show();});
        btn(c,"Encerrar sessão",()->new AlertDialog.Builder(a).setTitle("Encerrar sem concluir?").setNegativeButton("Continuar",null).setPositiveButton("Encerrar",(d,w)->finish(false)).show());
        wake(true);refresh();
    }
    FitnessData.Exercise current(){return FitnessData.GYM[group][exercise];}
    int estimatedSeconds(){FitnessData.Exercise e=current();String rep=e.rep(phase,set);int count=0;for(String p:rep.split("\\+")){if(p.contains("–"))p=p.substring(p.indexOf("–")+1);try{count+=Integer.parseInt(p);}catch(Exception ignored){}}if(count<=0)count=10;return count*(1+Integer.parseInt(e.tempo[phase].substring(2)));}
    void refresh(){
        if(mode.isEmpty()||timer==null)return;
        if(running){remaining=Math.max(0,end-SystemClock.elapsedRealtime());int sec=(int)((remaining+999)/1000);if(sec<=3&&sec>0&&sec!=lastAlert){lastAlert=sec;buzz(90);}if(remaining==0){running=false;handler.removeCallbacks(tick);buzz(350);if(mode.equals("run")){nextRun();return;}if(resting){nextSet();return;}}}
        timer.setText(time(remaining));
        if(mode.equals("run")){
            FitnessData.Step s=steps.get(index);stage.setText(s.name);detail.setText("Etapa "+(index+1)+" de "+steps.size()+" • "+(s.seconds==0?"Distância manual":s.seconds/60+" min"));
            String upcoming=index+1<steps.size()?steps.get(index+1).name:"Fim do treino";
            next.setText((running&&remaining<=3000?((remaining+999)/1000)+" • ":"")+"Próximo: "+upcoming);
            play.setText(running?"Pausar":"Retomar");play.setEnabled(s.seconds>0);advance.setText(s.seconds==0?"Concluir distância":"Pular etapa");
        }else{
            FitnessData.Exercise e=current();stage.setText(e.name);detail.setText("Exercício "+(exercise+1)+"/"+FitnessData.GYM[group].length+" • Série "+(set+1)+"/"+e.sets+"\n"+e.tech+" • "+e.rep(phase,set)+" reps • "+e.tempo[phase]+" • descanso "+e.rest[phase]+"s");
            next.setText(resting?"Descanso • próxima série/exercício":"Execução estimada • confirme a série manualmente");
            play.setText(running?"Pausar":remaining>0?"Retomar":"Iniciar tempo estimado");play.setEnabled(!resting||remaining>0);advance.setText(resting?"Terminar descanso":"Registrar série");
        }
    }
    void nextRun(){handler.removeCallbacks(tick);running=false;index++;if(index>=steps.size()){finish(true);return;}remaining=steps.get(index).seconds*1000L;if(remaining>0)startClock();refresh();}
    void recordSet(){
        pause();EditText load=new EditText(a);load.setInputType(8194);load.setHint("Carga em kg (opcional)");
        String key="gym_load_"+group+"_"+exercise;load.setText(prefs.getString(key,""));
        new AlertDialog.Builder(a).setTitle("Série realizada").setMessage("Registre a carga, se desejar.").setView(load).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{prefs.edit().putString(key,load.getText().toString()).putString("gym_log_"+System.currentTimeMillis(),current().name+" • "+(set+1)+" • "+load.getText()).apply();if(exercise==FitnessData.GYM[group].length-1&&set==current().sets-1){finish(true);return;}resting=true;remaining=current().rest[phase]*1000L;startClock();refresh();}).show();
    }
    void nextSet(){handler.removeCallbacks(tick);running=false;resting=false;remaining=0;set++;if(set>=current().sets){exercise++;set=0;}if(exercise>=FitnessData.GYM[group].length){finish(true);return;}refresh();}
    void finish(boolean completed){handler.removeCallbacks(tick);running=false;wake(false);if(completed){if(mode.equals("run"))prefs.edit().putBoolean("run_done_"+week+"_"+day,true).apply();else prefs.edit().putString("gym_done_"+group,LocalDate.now().toString()).apply();}mode="";timer=null;a.render();if(completed)Toast.makeText(a,"Treino concluído!",Toast.LENGTH_LONG).show();}
    void destroy(){handler.removeCallbacks(tick);wake(false);}
    void profile(LinearLayout c){
        float w=PlanData.weight(a),h=prefs.getFloat("fit_height",0),age=prefs.getFloat("fit_age",0);int sex=prefs.getInt("fit_sex",-1);
        line(c,"Peso atual: "+PlanData.fmt(w)+" kg");
        if(h>0&&age>0&&sex>=0){double basal=10*w+6.25*h-5*age+(sex==0?5:-161);line(c,"Taxa metabólica basal estimada: "+Math.round(basal)+" kcal/dia • Mifflin–St Jeor");line(c,"IMC: "+String.format(Locale.US,"%.1f",w/Math.pow(h/100,2))+" • Não mede gordura corporal.");}
        else line(c,"Preencha altura, idade e sexo usado na fórmula para calcular a taxa basal.");
        line(c,"TMB não é gasto total diário nem meta de ingestão. Peso e medidas não distinguem gordura de massa magra.");
        btn(c,"Registrar peso e medidas",()->editProfile());
        btn(c,"Histórico de medidas",()->{TreeMap<String,String> history=new TreeMap<>();for(Map.Entry<String,?> e:prefs.getAll().entrySet())if(e.getKey().startsWith("fit_measure_"))history.put(e.getKey().substring(12),String.valueOf(e.getValue()));StringBuilder s=new StringBuilder();for(Map.Entry<String,String> e:history.descendingMap().entrySet())s.append(e.getKey()).append("\n").append(e.getValue()).append("\n\n");new AlertDialog.Builder(a).setTitle("Histórico").setMessage(s.length()==0?"Nenhum registro":s.toString()).setPositiveButton("Fechar",null).show();});
    }
    EditText field(LinearLayout c,String title,float value){line(c,title);EditText t=new EditText(a);t.setInputType(8194);t.setSingleLine();if(value>0)t.setText(Float.toString(value));c.addView(t);return t;}
    float number(EditText t){String s=t.getText().toString().trim().replace(',','.');return s.isEmpty()?0:Float.parseFloat(s);}
    void editProfile(){
        LinearLayout c=new LinearLayout(a);c.setOrientation(1);c.setPadding(dp(16),0,dp(16),0);ScrollView scroll=new ScrollView(a);scroll.addView(c);
        EditText w=field(c,"Peso (kg)",PlanData.weight(a)),h=field(c,"Altura (cm)",prefs.getFloat("fit_height",0)),age=field(c,"Idade (anos)",prefs.getFloat("fit_age",0));
        Spinner sex=new Spinner(a);ArrayAdapter<String> ad=new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"Selecione sexo da fórmula","Masculino","Feminino"});sex.setAdapter(ad);sex.setSelection(prefs.getInt("fit_sex",-1)+1);c.addView(sex);
        String[] labels={"Cintura","Abdômen","Quadril","Peito","Braço","Coxa","Panturrilha"};EditText[] measures=new EditText[labels.length];for(int i=0;i<labels.length;i++)measures[i]=field(c,labels[i]+" (cm, opcional)",prefs.getFloat("fit_m"+i,0));
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Avaliação corporal").setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();d.show();d.getButton(-1).setOnClickListener(v->{try{float ww=number(w),hh=number(h),aa=number(age);if(!Float.isFinite(ww)||!Float.isFinite(hh)||!Float.isFinite(aa)||ww<30||ww>350||hh<100||hh>250||aa<18||aa>100||sex.getSelectedItemPosition()==0)throw new Exception();SharedPreferences.Editor ed=prefs.edit();StringBuilder s=new StringBuilder(PlanData.fmt(ww)+" kg");for(int i=0;i<labels.length;i++){float n=number(measures[i]);if(!Float.isFinite(n)||n<0||n>300)throw new Exception();ed.putFloat("fit_m"+i,n);if(n>0)s.append(" • ").append(labels[i]).append(": ").append(n).append(" cm");}ed.putFloat("fit_height",hh).putFloat("fit_age",aa).putInt("fit_sex",sex.getSelectedItemPosition()-1).putString("fit_measure_"+LocalDate.now(),s.toString()).apply();PlanData.setWeight(a,ww);ReminderScheduler.setup(a);d.dismiss();a.render();}catch(Exception ex){Toast.makeText(a,"Confira os valores e selecione o sexo da fórmula.",Toast.LENGTH_LONG).show();}});
    }
}