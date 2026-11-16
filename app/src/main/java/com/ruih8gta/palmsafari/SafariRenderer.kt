package com.ruih8gta.palmsafari

import android.opengl.GLES11Ext
import android.opengl.GLES20 as GL
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.google.ar.core.Anchor
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SafariRenderer(
    private val creatures: List<Creature>,
    private val report: (String) -> Unit,
) : GLSurfaceView.Renderer {
    @Volatile var settings = SceneSettings()
    @Volatile var session: Session? = null
    @Volatile var displayRotation = 0
    private val taps = AtomicReference<Pair<Float,Float>?>(null)
    private var anchor: Anchor? = null
    private var width = 1
    private var height = 1
    private var cameraTexture = 0
    private var backgroundProgram = 0
    private var modelProgram = 0
    private lateinit var sphere: GlMesh
    private lateinit var cone: GlMesh
    private lateinit var ring: GlMesh
    private var failed = false
    private var lastReport = ""
    private val clock = MotionClock()
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val root = FloatArray(16)
    private val joint = FloatArray(16)
    private val part = FloatArray(16)
    private val mvp = FloatArray(16)
    private val inverse = FloatArray(16)
    private val normal = FloatArray(16)
    private val quad = buffer(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f))
    private val uv = buffer(FloatArray(8))
    private var posLocation=0; private var normalLocation=0; private var mvpLocation=0
    private var normalMatrixLocation=0; private var colorLocation=0
    private var bgPosition=0; private var bgUv=0; private var bgSampler=0

    fun tap(x: Float, y: Float) { taps.set(x to y) }
    /** Call on GL thread, or after GLSurfaceView.onPause has joined it. */
    fun resetPlacement() { anchor?.detach(); anchor=null; taps.set(null); failed=false; clock.suspendClock(); lastReport="" }
    fun suspendClock() { clock.suspendClock() }
    private fun status(text: String) { if(text != lastReport) { lastReport=text; report(text) } }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        failed = false
        try {
            modelProgram = program("""
                uniform mat4 uMvp; uniform mat4 uNormal;
                attribute vec3 aPosition; attribute vec3 aNormal;
                varying vec3 vNormal;
                void main(){ gl_Position=uMvp*vec4(aPosition,1.0); vNormal=(uNormal*vec4(aNormal,0.0)).xyz; }
            """, """
                precision mediump float; uniform vec4 uColor; varying vec3 vNormal;
                void main(){ float light=.52+.48*max(dot(normalize(vNormal),normalize(vec3(-.4,1.0,.7))),0.0); gl_FragColor=vec4(uColor.rgb*light,uColor.a); }
            """)
            posLocation=GL.glGetAttribLocation(modelProgram,"aPosition"); normalLocation=GL.glGetAttribLocation(modelProgram,"aNormal")
            mvpLocation=GL.glGetUniformLocation(modelProgram,"uMvp"); normalMatrixLocation=GL.glGetUniformLocation(modelProgram,"uNormal")
            colorLocation=GL.glGetUniformLocation(modelProgram,"uColor")
            backgroundProgram=program("""
                attribute vec2 aPosition; attribute vec2 aUv; varying vec2 vUv;
                void main(){ gl_Position=vec4(aPosition,0.0,1.0); vUv=aUv; }
            """, """
                #extension GL_OES_EGL_image_external : require
                precision mediump float; uniform samplerExternalOES uTexture; varying vec2 vUv;
                void main(){ gl_FragColor=texture2D(uTexture,vUv); }
            """)
            bgPosition=GL.glGetAttribLocation(backgroundProgram,"aPosition"); bgUv=GL.glGetAttribLocation(backgroundProgram,"aUv")
            bgSampler=GL.glGetUniformLocation(backgroundProgram,"uTexture")
            val names=IntArray(1);GL.glGenTextures(1,names,0);cameraTexture=names[0]
            GL.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,cameraTexture)
            GL.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GL.GL_TEXTURE_MIN_FILTER,GL.GL_LINEAR)
            GL.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GL.GL_TEXTURE_MAG_FILTER,GL.GL_LINEAR)
            GL.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GL.GL_TEXTURE_WRAP_S,GL.GL_CLAMP_TO_EDGE)
            GL.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GL.GL_TEXTURE_WRAP_T,GL.GL_CLAMP_TO_EDGE)
            sphere=GlMesh(MeshGeometry.sphere());cone=GlMesh(MeshGeometry.cone());ring=GlMesh(MeshGeometry.ring())
            GL.glDisable(GL.GL_CULL_FACE)
        } catch(e: RuntimeException) { failed=true;status("3D描画を開始できませんでした。アプリを開き直してください。") }
    }
    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) { width=w.coerceAtLeast(1);height=h.coerceAtLeast(1);GL.glViewport(0,0,width,height) }

    override fun onDrawFrame(gl: GL10?) {
        GL.glClearColor(.86f,.9f,.84f,1f);GL.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)
        if(failed) return
        val state=settings.normalized()
        try {
            val ar=session
            if(ar == null) {
                clock.tick(System.nanoTime(),state.motion)
                Matrix.perspectiveM(projection,0,35f,width.toFloat()/height,.1f,40f)
                Matrix.setLookAtM(view,0,2.6f,2.1f,4.6f,0f,.64f,0f,0f,1f,0f)
                Matrix.multiplyMM(vp,0,projection,0,view,0)
                Matrix.setIdentityM(root,0)
                drawRing(root,1.43f,floatArrayOf(.57f,.67f,.5f,1f))
                Matrix.setIdentityM(root,0)
                Matrix.scaleM(root,0,.85f,.85f,.85f)
                status("3Dプレビュー · カメラは使用していません")
            } else {
                ar.setCameraTextureName(cameraTexture)
                ar.setDisplayGeometry(displayRotation,width,height)
                val frame=ar.update()
                drawCamera(frame)
                if(frame.camera.trackingState != TrackingState.TRACKING) {
                    taps.set(null);clock.suspendClock()
                    status("明るい場所で、模様のある床や机に向けてゆっくり動かしてください")
                    return
                }
                frame.camera.getProjectionMatrix(projection,0,.05f,100f)
                frame.camera.getViewMatrix(view,0)
                Matrix.multiplyMM(vp,0,projection,0,view,0)
                val tap=taps.getAndSet(null)
                if(anchor == null && tap != null) {
                    val hit=planeHit(frame,tap.first,tap.second)
                    if(hit != null) anchor=hit.createAnchor()
                }
                val placed=anchor
                if(placed == null) {
                    clock.suspendClock()
                    val hit=planeHit(frame,width/2f,height/2f)
                    if(hit != null) {
                        hit.hitPose.toMatrix(root,0)
                        drawRing(root,.12f,floatArrayOf(.4f,.9f,.6f,1f))
                        status("リング付近の床や机をタップして置いてください")
                    } else status("床や机を探しています · カメラをゆっくり動かしてください")
                    return
                }
                if(placed.trackingState != TrackingState.TRACKING) {
                    clock.suspendClock();status("置いた場所を見失いました。元の場所へカメラを向けるか、置き直してください");return
                }
                clock.tick(System.nanoTime(),state.motion)
                placed.pose.toMatrix(root,0)
                Matrix.scaleM(root,0,.28f,.28f,.28f)
                status("配置しました · スマホを動かすと別の角度から見られます")
            }
            Matrix.scaleM(root,0,state.size,state.size,state.size)
            Matrix.rotateM(root,0,state.angle,0f,1f,0f)
            Matrix.translateM(root,0,0f,AnimationPose.rootY(clock.seconds,clock.pose),0f)
            drawCreature(creatures[state.model])
        } catch(e: com.google.ar.core.exceptions.CameraNotAvailableException) {
            failed=true;status("カメラを利用できません。他のカメラアプリを閉じて、ARを開き直してください")
        } catch(e: RuntimeException) {
            failed=true;status("表示を続けられません。プレビューに戻り、もう一度ARを開始してください")
        }
    }

    private fun planeHit(frame: Frame, x: Float, y: Float) = frame.hitTest(x,y).firstOrNull { hit ->
        val plane=hit.trackable as? Plane
        plane != null && plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
            plane.trackingState == TrackingState.TRACKING && plane.isPoseInPolygon(hit.hitPose)
    }

    private fun drawCamera(frame: Frame) {
        if(frame.timestamp == 0L) return
        quad.position(0);uv.position(0)
        frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,quad,Coordinates2d.TEXTURE_NORMALIZED,uv)
        GL.glDisable(GL.GL_DEPTH_TEST);GL.glDepthMask(false)
        GL.glUseProgram(backgroundProgram);GL.glActiveTexture(GL.GL_TEXTURE0)
        GL.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,cameraTexture);GL.glUniform1i(bgSampler,0)
        quad.position(0);uv.position(0)
        GL.glVertexAttribPointer(bgPosition,2,GL.GL_FLOAT,false,0,quad)
        GL.glVertexAttribPointer(bgUv,2,GL.GL_FLOAT,false,0,uv)
        GL.glEnableVertexAttribArray(bgPosition);GL.glEnableVertexAttribArray(bgUv)
        GL.glDrawArrays(GL.GL_TRIANGLE_STRIP,0,4)
        GL.glDisableVertexAttribArray(bgPosition);GL.glDisableVertexAttribArray(bgUv)
        GL.glDepthMask(true)
    }
    private fun drawRing(transform: FloatArray, scale: Float, color: FloatArray) {
        val matrix=transform.copyOf();Matrix.scaleM(matrix,0,scale,scale,scale);drawMesh(ring,matrix,color)
    }
    private fun drawCreature(creature: Creature) {
        for(g in creature.joints) {
            val pose=AnimationPose.joint(g.id,clock.seconds,clock.pose)
            root.copyInto(joint)
            Matrix.translateM(joint,0,g.pivot[0],g.pivot[1]+pose.y,g.pivot[2]+pose.z)
            Matrix.rotateM(joint,0,pose.rotateX,1f,0f,0f);Matrix.rotateM(joint,0,pose.rotateY,0f,1f,0f)
            Matrix.scaleM(joint,0,1f,pose.scaleY,1f)
            for(p in g.parts) {
                joint.copyInto(part);Matrix.translateM(part,0,p.position[0],p.position[1],p.position[2])
                Matrix.rotateM(part,0,p.rotation,0f,0f,1f);Matrix.scaleM(part,0,p.scale[0],p.scale[1],p.scale[2])
                drawMesh(if(p.cone) cone else sphere,part,p.color)
            }
        }
    }
    private fun drawMesh(mesh: GlMesh, transform: FloatArray, color: FloatArray) {
        GL.glEnable(GL.GL_DEPTH_TEST);GL.glUseProgram(modelProgram)
        Matrix.multiplyMM(mvp,0,vp,0,transform,0)
        Matrix.invertM(inverse,0,transform,0);Matrix.transposeM(normal,0,inverse,0)
        GL.glUniformMatrix4fv(mvpLocation,1,false,mvp,0);GL.glUniformMatrix4fv(normalMatrixLocation,1,false,normal,0)
        GL.glUniform4fv(colorLocation,1,color,0)
        mesh.vertices.position(0);GL.glVertexAttribPointer(posLocation,3,GL.GL_FLOAT,false,24,mesh.vertices)
        mesh.vertices.position(3);GL.glVertexAttribPointer(normalLocation,3,GL.GL_FLOAT,false,24,mesh.vertices)
        GL.glEnableVertexAttribArray(posLocation);GL.glEnableVertexAttribArray(normalLocation)
        GL.glDrawArrays(GL.GL_TRIANGLES,0,mesh.count)
        GL.glDisableVertexAttribArray(posLocation);GL.glDisableVertexAttribArray(normalLocation)
    }
    private class GlMesh(data: FloatArray) { val vertices=buffer(data);val count=data.size/6 }
    companion object {
        private fun buffer(data: FloatArray): FloatBuffer = ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data);position(0) }
        private fun shader(type: Int, source: String): Int {
            val id=GL.glCreateShader(type);GL.glShaderSource(id,source.trimIndent());GL.glCompileShader(id)
            val ok=IntArray(1);GL.glGetShaderiv(id,GL.GL_COMPILE_STATUS,ok,0)
            check(ok[0] != 0) { GL.glGetShaderInfoLog(id) };return id
        }
        private fun program(vertex: String, fragment: String): Int {
            val v=shader(GL.GL_VERTEX_SHADER,vertex);val f=shader(GL.GL_FRAGMENT_SHADER,fragment)
            val id=GL.glCreateProgram();GL.glAttachShader(id,v);GL.glAttachShader(id,f);GL.glLinkProgram(id)
            val ok=IntArray(1);GL.glGetProgramiv(id,GL.GL_LINK_STATUS,ok,0)
            GL.glDeleteShader(v);GL.glDeleteShader(f);check(ok[0] != 0) { GL.glGetProgramInfoLog(id) };return id
        }
    }
}
