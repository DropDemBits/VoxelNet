package ddb.io.voxelnet.render;

import ddb.io.voxelnet.entity.EntityPlayer;
import org.joml.Matrix4f;

public class Camera
{
	// Position related
	public float x = 0.0f;
	public float y = 0.0f;
	public float z = 0.0f;
	
	public float pitch = 0.0f;
	public float yaw = 0.0f;
	
	// Perspective related
	public float fov = 60.0f;
	public float zNear = 0.01f;
	public float zFar = 1000f;
	
	// Matrices
	public Matrix4f perspectiveMatrix;
	public Matrix4f viewMatrix;
	
	public Camera()
	{
		this.perspectiveMatrix = new Matrix4f();
		this.viewMatrix = new Matrix4f();
	}
	
	public void setPosition(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void setOrientaion(float pitch, float yaw)
	{
		this.pitch = pitch;
		this.yaw = yaw;
	}
	
	/**
	 * Updates the perspective matrix with the given aspect ratio
	 * @param aspect The new aspect ratio for the screen
	 */
	public void updatePerspective(float aspect)
	{
		this.perspectiveMatrix.identity();
		this.perspectiveMatrix.perspective((float) Math.toRadians(fov), aspect, zNear, zFar);
	}
	
	/**
	 * Updates the view matrix
	 */
	public void updateView()
	{
		this.viewMatrix.identity();
		this.viewMatrix.rotate((float) -Math.toRadians(pitch), 1.0f, 0.0f, 0.0f);
		this.viewMatrix.rotate((float) -Math.toRadians(yaw), 0.0f, 1.0f, 0.0f);
		this.viewMatrix.translate(-x, -y, -z);
	}
	
	/**
	 * Updates the camera position to be the same as the player
	 * @param player The player to set the camera position to
	 */
	public void asPlayer(EntityPlayer player)
	{
		this.x = player.xPos;
		this.y = player.yPos;
		this.z = player.zPos;
		
		this.pitch = player.pitch;
		this.yaw = player.yaw;
	}
	
	/**
	 * Gets the actual transform of the camera
	 * The matrix returned is the combined perspective-view matrix
	 * @return The actual transformation matrix of the camera
	 */
	public Matrix4f getTransform()
	{
		return new Matrix4f().identity().mul(perspectiveMatrix).mul(viewMatrix);
	}
}