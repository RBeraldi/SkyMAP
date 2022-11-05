package com.beraldi.skymap

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View


import android.opengl.Matrix.*
import android.util.Log
import java.lang.Math.*
import java.util.logging.Logger



class MainActivity : AppCompatActivity(), SensorEventListener {

    private var mLastAccelerometer = FloatArray(3)
    private var mLastMagnetometer = FloatArray(3)
    private var mRotationVector = FloatArray(3)
    private var mRotMatrix = FloatArray(16)
    lateinit var sensorManager : SensorManager

    //Direction to show in the Earth frame, for example Polaris
    val lat = (Math.PI*42/180)
    private var D = floatArrayOf(0f,cos(lat).toFloat(),sin(lat).toFloat(),0f)
    //Cardinal directions in the Earth frame
    private var N = floatArrayOf(0f,1f,0.0f,0f)
    private var E = floatArrayOf(1f,0f,0.0f,0f)
    private var S = floatArrayOf(0f,-1f,0.0f,0f)
    private var W = floatArrayOf(-1f,0f,0.0f,0f)

    //Direction to show in the camera frame
    private var DC = floatArrayOf(0f,0f,0f,0f)

    //Cardinal directions in the camera frame
    private var NC = floatArrayOf(0f,1f,0.0f,0f)
    private var SC = floatArrayOf(0f,-1f,0.0f,0f)

    private var EC = floatArrayOf(1f,0f,0.0f,0f)
    private var WC = floatArrayOf(-1f,0f,0.0f,0f)

    //Vanishing Point on the screen
    var VPX = 0f; var VPY = 0f
    //Vanishing Point on the screen
    var VPNX = 0f; var VPNY = 0f //VP North
    var VPSX = 0f; var VPSY = 0f //VP South
    var VPEX = 0f; var VPEY = 0f //VP East
    var VPWX = 0f; var VPWY = 0f //VP West

    var VP = floatArrayOf(0f,0f)

    var H = floatArrayOf(0f,0f,0f) //Orizzonte

    val v1 = floatArrayOf(1f,1f,1f,1f)
    val v2 = floatArrayOf(2f,1f,1f,1f)
    val v3 = floatArrayOf(1.5f,1f,2f,1f)

    var vv1 = FloatArray(4)


    var logger = Logger()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SkyView(this))
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

    }
    override fun onStart() {
        super.onStart()
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onSensorChanged(event: SensorEvent?) {

        if (event!!.sensor.type== Sensor.TYPE_ROTATION_VECTOR){
            mRotationVector = event.values.clone()
        }
        SensorManager.getRotationMatrixFromVector(mRotMatrix,mRotationVector)
        H[0]=mRotMatrix[2]
        H[1]=mRotMatrix[6]
        H[2]=mRotMatrix[10]
        logger.logRotationMatrix()
        //SensorManager.getRotationMatrix(mRotMatrix,null,mLastAccelerometer,mLastMagnetometer)

    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //TODO("Not yet implemented")
    }

    inner class Logger(){
        val logevery=45
        var i=0
        fun logRotationMatrix(){
            if (i%logevery!=0) {
                i++
                return
            }
            i++
            Log.i("LOG:","ROTATION MATRIX:\n"+
                    mRotMatrix[0]+" "+ mRotMatrix[1]+" "+ mRotMatrix[2]+" "+ mRotMatrix[3]+"\n"+
                    mRotMatrix[4]+" "+ mRotMatrix[5]+" "+ mRotMatrix[6]+" "+ mRotMatrix[7]+"\n"+
                    mRotMatrix[8]+" "+ mRotMatrix[9]+" "+ mRotMatrix[10]+" "+ mRotMatrix[11]+"\n"+
                    mRotMatrix[12]+" "+ mRotMatrix[13]+" "+ mRotMatrix[14]+" "+ mRotMatrix[15]+"\n"

            )
        }
    }

    inner class SkyView(context: Context) : View(context, null) {

        //Intrinsic Parameters Matrix
        val w=1440f
        val h=2418f//2880f
        val f = 1f //Focal length in mm, AoV=90

        val sx=w/2
        val sy=-h/2
        val tx = w/2
        val ty = h/2

        val mPaint = Paint().apply {
            style= Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth=5f
            isAntiAlias = true
        }
        val mTextPaint = Paint().apply {
            style= Paint.Style.FILL
            color = Color.WHITE
            textSize=150f
            isAntiAlias = true
        }
        val mLogTextPaint = Paint().apply {
            style= Paint.Style.FILL
            color = Color.WHITE
            textSize=50f
            isAntiAlias = true
        }
        //Coordinate of the line in the screen reference frame
        var x0=0f; var y0=0f
        var x1=1f; var y1=1f
        var T = Matrix().apply { setTranslate(tx,ty) }
        var K = Matrix().apply {
            setScale(sx,sy)
            postConcat(T)
            }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawARGB(255,0,0,0)
            //canvas.drawText("XYZ:"+ -H[0]+" "+-H[1]+" "+-H[2],50f,50f,mLogTextPaint)
           // canvas.drawText("A:"+ atan2(-H[2].toDouble(),-H[1].toDouble()),50f,100f,mLogTextPaint)

            //Step 1. TRASFORMA LA DIREZIONE IN COORDINATE DELLA CAMERA
            //android.opengl.Matrix.
            //Moltiplica Direzione per rotation matrix trasposta
            //openGL memorizza per colonne
            multiplyMV(DC,0,mRotMatrix,0,D,0)
            multiplyMV(NC,0,mRotMatrix,0,N,0)
            multiplyMV(EC,0,mRotMatrix,0,E,0)
            multiplyMV(WC,0,mRotMatrix,0,W,0)
            multiplyMV(SC,0,mRotMatrix,0,S,0)

            //Trasforma il triangolo
            multiplyMV(vv1,0,mRotMatrix,0,v1,0)


            //2. PROIETTA E CALCOLA I VANISHING POINTS
            //-z perche' il piano di proiezione punta verso -Z
            VPX=f*DC[0]/-DC[2]
            VPY=f*DC[1]/-DC[2]

            VPNX=f*NC[0]/-NC[2]
            VPNY=f*NC[1]/-NC[2]

            VPEX=f*EC[0]/-EC[2]
            VPEY=f*EC[1]/-EC[2]

            VPSX=f*SC[0]/-SC[2]
            VPSY=f*SC[1]/-SC[2]

            VPWX=f*WC[0]/-WC[2]
            VPWY=f*WC[1]/-WC[2]




            //3. APPLICA PARAMETRI INTRINSECI DELLA CAMERA
            //Log.i("VPX: ",VPX.toString()+" "+VPY.toString())

            VP[0]=VPX;VP[1]=VPY
            K.mapPoints(VP,VP)

            VPX=sx*VPX+w/2 //Scaling e traslazione
            VPY=sy*VPY+h/2//Scaling, mirror e traslazione

            canvas.drawText("VP Polaris X:"+ VPX+ " Y: "+VPY,50f,1500f,mLogTextPaint)

            VPNX=sx*VPNX+w/2 //Scaling e traslazione
            VPNY=sy*VPNY+h/2//Scaling, mirror e traslazione

//            canvas.drawText("VP North-Y:"+ VPNY,50f,200f,mLogTextPaint)


            VPEX=sx*VPEX+w/2 //Scaling e traslazione
            VPEY=sy*VPEY+h/2//Scaling, mirror e traslazione

            VPSX=sx*VPSX+w/2 //Scaling e traslazione
            VPSY=sy*VPSY+h/2//Scaling, mirror e traslazione

            VPWX=sx*VPWX+w/2 //Scaling e traslazione
            VPWY=sy*VPWY+h/2//Scaling, mirror e traslazione


            //Mostra solo gli oggetti che sono davanti
            if (DC[2]<0f) {
                canvas.drawCircle(VPX,VPY,25f,mPaint)
                //canvas.drawCircle(VP[0],VP[1],25f,mPaint)
                canvas.drawText("Polaris",VPX,VPY,mLogTextPaint)
            }

            if (NC[2]<0f) canvas.drawText("N",VPNX,VPNY,mTextPaint)
            if (EC[2]<0f) canvas.drawText("E",VPEX,VPEY,mTextPaint)
            if (SC[2]<0f) canvas.drawText("S",VPSX,VPSY,mTextPaint)
            if (WC[2]<0f) canvas.drawText("W",VPWX,VPWY,mTextPaint)

            x0=0f;y0=h/2;x1=w;y1=h/2
            canvas.drawLine(x0,y0,x1,y1,mPaint)

            invalidate()

        }


    }
}