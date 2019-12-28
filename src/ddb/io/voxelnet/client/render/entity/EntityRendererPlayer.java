package ddb.io.voxelnet.client.render.entity;

import ddb.io.voxelnet.client.render.*;
import ddb.io.voxelnet.client.render.gl.EnumDrawMode;
import ddb.io.voxelnet.entity.Entity;
import org.joml.Matrix4f;

public class EntityRendererPlayer extends EntityRenderer
{
	private final Model playerModel;
	private final ModelBuilder playerBuilder;
	private final Texture playerTexture;
	
	public EntityRendererPlayer()
	{
		// Use block layout since that's what "default" shader uses
		// TODO: Move & rename layout to something else
		playerModel = new Model(BlockRenderer.BLOCK_LAYOUT);
		playerBuilder = new ModelBuilder(BlockRenderer.BLOCK_LAYOUT, EnumDrawMode.TRIANGLES);
		
		// Load texture that should really be loaded by somewhere else
		playerTexture = new Texture("assets/textures/test-player.png");
		
		// Build crude player model
		playerBuilder.reset();
		// Body
		/*addCube(0f, 0f, 0f,
				16,16,16,
				0, 0,
				playerBuilder, playerTexture);*/
		// Head
		addCube(-0.5f, 1.00001f, -0.5f,
				1f,1f, 1f,
				0f, 0,
				32,32,16,
				playerBuilder, playerTexture);
		
		playerModel.bind();
		playerBuilder.compact();
		playerModel.updateVertices(playerBuilder);
		playerModel.unbind();
	}
	
	/**
	 * Adds a cube to the mesh
	 * All offsets are in cube units
	 * All sizes & uv coordinates are in pixels
	 *
	 * @param xOff
	 * @param yOff
	 * @param zOff
	 * @param xSize
	 * @param ySize
	 * @param zSize
	 * @param uStart
	 * @param vStart
	 */
	private void addCube(float xOff, float yOff, float zOff,
	                     float xSize, float ySize, float zSize,
	                     float uStart, float vStart,
	                     float width, float height, float depth,
	                     ModelBuilder builder, Texture texture)
	{
		// Scales for the uv coordinates
		float uScale = texture.getWidth() * 0xFFFF;
		float vScale = texture.getHeight() * 0xFFFF;
		byte skyLight = (byte)255;
		byte blockLight = (byte)255;
		byte aoLight = 0;
		
		// Positions & offsets for the uv coordinates
		float uOff, vOff;
		short uMin, vMin;
		short uMax, vMax;
		
		// Front face
		uOff = uStart + depth;
		vOff = vStart + depth;
		
		uMin = (short)0x0000;//((uOff) * uScale);
		vMin = (short)0x0000;//((vOff) * vScale);
		uMax = (short)0xFFFF;//((uOff + width) * uScale);
		vMax = (short)0xFFFF;//((vOff + height) * vScale);
		
		builder.addPoly(4);
		builder.pos3f(xOff + 0.0f * xSize, yOff + 1.0f * ySize, zOff + 0.0f * zSize).tex2i(uMax, vMax).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 1.0f * xSize, yOff + 1.0f * ySize, zOff + 0.0f * zSize).tex2i(uMin, vMax).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 1.0f * xSize, yOff + 0.0f * ySize, zOff + 0.0f * zSize).tex2i(uMin, vMin).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 0.0f * xSize, yOff + 0.0f * ySize, zOff + 0.0f * zSize).tex2i(uMax, vMin).light3b(skyLight, blockLight, aoLight).endVertex();
		
		// Back face
		builder.addPoly(4);
		builder.pos3f(xOff + 0.0f * xSize, yOff + 0.0f * ySize, zOff + 1.0f * zSize).tex2i(uMin, vMin).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 1.0f * xSize, yOff + 0.0f * ySize, zOff + 1.0f * zSize).tex2i(uMax, vMin).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 1.0f * xSize, yOff + 1.0f * ySize, zOff + 1.0f * zSize).tex2i(uMax, vMax).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 0.0f * xSize, yOff + 1.0f * ySize, zOff + 1.0f * zSize).tex2i(uMin, vMax).light3b(skyLight, blockLight, aoLight).endVertex();
		
		// Left face
		builder.addPoly(4);
		builder.pos3f(xOff + 0.0f * xSize, yOff + 0.0f * ySize, zOff + 0.0f * zSize).tex2i(uMin, vMin).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 0.0f * xSize, yOff + 0.0f * ySize, zOff + 1.0f * zSize).tex2i(uMax, vMin).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 0.0f * xSize, yOff + 1.0f * ySize, zOff + 1.0f * zSize).tex2i(uMax, vMax).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 0.0f * xSize, yOff + 1.0f * ySize, zOff + 0.0f * zSize).tex2i(uMin, vMax).light3b(skyLight, blockLight, aoLight).endVertex();
		
		// Right face
		builder.addPoly(4);
		builder.pos3f(xOff + 1.0f * xSize, yOff + 1.0f * ySize, zOff + 0.0f * zSize).tex2i(uMax, vMax).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 1.0f * xSize, yOff + 1.0f * ySize, zOff + 1.0f * zSize).tex2i(uMin, vMax).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 1.0f * xSize, yOff + 0.0f * ySize, zOff + 1.0f * zSize).tex2i(uMin, vMin).light3b(skyLight, blockLight, aoLight).endVertex();
		builder.pos3f(xOff + 1.0f * xSize, yOff + 0.0f * ySize, zOff + 0.0f * zSize).tex2i(uMax, vMin).light3b(skyLight, blockLight, aoLight).endVertex();
	}
	
	@Override
	public void render(Entity e, GameRenderer renderer)
	{
		Matrix4f playerTransform = playerModel.getTransform().identity();
		playerTransform.identity();
		
		// Currently setup so that the entire player model pitches
		playerTransform.translate(e.xPos, e.yPos, e.zPos);
		//playerTransform.translate(-0.5f, 0.0f, -0.5f);
		playerTransform.rotate((float)Math.toRadians(e.yaw), 0.f, 1.f, 0.f);
		playerTransform.rotate((float)Math.toRadians(e.pitch), 1.f, 0.f, 0.f);
		//playerTransform.translate(0.5f, 0.0f, 0.5f);
		
		// Hacky: Bind to texture slot #2
		// We do this as there is no good way currently of attaching textures
		// to specific models / renderers
		// Change the shader's sampler slot to allow for actually using this texture
		playerTexture.bind(2);
		renderer.getCurrentShader().setUniform1i("texture0", 2);
		
		renderer.drawModel(playerModel);
		
		playerTexture.unbind();
		renderer.getCurrentShader().setUniform1i("texture0", 0);
		
	}
	
}
