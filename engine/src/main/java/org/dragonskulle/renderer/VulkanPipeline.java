/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.logging.Logger;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

/**
 * Class abstracting a vulkan pipeline
 *
 * @author Aurimas Blažulionis
 */
class VulkanPipeline {
    public long pipeline;
    public long layout;

    private VkDevice mDevice;

    public static final Logger LOGGER = Logger.getLogger("render");

    public VulkanPipeline(
            ShaderBuf vertShaderBuf,
            ShaderBuf fragShaderBuf,
            VkDevice device,
            VkExtent2D extent,
            long descriptorSetLayout,
            long renderPass) {
        LOGGER.info("Setup pipeline");

        mDevice = device;

        Shader vertShader = Shader.getShader(vertShaderBuf, mDevice);

        if (vertShader == null) throw new RuntimeException("Failed to retrieve vertex shader!");

        Shader fragShader = Shader.getShader(fragShaderBuf, mDevice);

        if (fragShader == null) throw new RuntimeException("Failed to retrieve fragment shader!");

        try (MemoryStack stack = stackPush()) {
            // Programmable pipelines

            VkPipelineShaderStageCreateInfo.Buffer shaderStages =
                    VkPipelineShaderStageCreateInfo.callocStack(2, stack);

            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);
            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShader.getModule());
            vertShaderStageInfo.pName(stack.UTF8("main"));
            // We will need pSpecializationInfo here to configure constants

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);
            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShader.getModule());
            fragShaderStageInfo.pName(stack.UTF8("main"));

            // Fixed function pipelines

            VkPipelineVertexInputStateCreateInfo vertexInputInfo =
                    VkPipelineVertexInputStateCreateInfo.callocStack(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(Vertex.getBindingDescription(stack));
            vertexInputInfo.pVertexAttributeDescriptions(Vertex.getAttributeDescriptions(stack));

            VkPipelineInputAssemblyStateCreateInfo inputAssembly =
                    VkPipelineInputAssemblyStateCreateInfo.callocStack(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            VkViewport.Buffer viewport = VkViewport.callocStack(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width((float) extent.width());
            viewport.height((float) extent.height());
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            // Render entire viewport at once
            VkRect2D.Buffer scissor = VkRect2D.callocStack(1, stack);
            scissor.offset().x(0);
            scissor.offset().y(0);
            scissor.extent(extent);

            VkPipelineViewportStateCreateInfo viewportState =
                    VkPipelineViewportStateCreateInfo.callocStack(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.pViewports(viewport);
            viewportState.pScissors(scissor);

            VkPipelineRasterizationStateCreateInfo rasterizer =
                    VkPipelineRasterizationStateCreateInfo.callocStack(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
            rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);

            // Used for shadowmaps, which we currently don't have...
            rasterizer.depthBiasEnable(false);

            // TODO: Enable MSAA once we check for features etc...
            VkPipelineMultisampleStateCreateInfo multisampling =
                    VkPipelineMultisampleStateCreateInfo.callocStack(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // TODO: Depth blend with VkPipelineDepthStencilStateCreateInfo

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                    VkPipelineColorBlendAttachmentState.callocStack(1, stack);
            colorBlendAttachment.colorWriteMask(
                    VK_COLOR_COMPONENT_R_BIT
                            | VK_COLOR_COMPONENT_G_BIT
                            | VK_COLOR_COMPONENT_B_BIT
                            | VK_COLOR_COMPONENT_A_BIT);
            colorBlendAttachment.blendEnable(false);

            VkPipelineColorBlendStateCreateInfo colorBlending =
                    VkPipelineColorBlendStateCreateInfo.callocStack(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.pAttachments(colorBlendAttachment);

            // TODO: Dynamic states

            VkPipelineLayoutCreateInfo pipelineLayoutInfo =
                    VkPipelineLayoutCreateInfo.callocStack(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            LongBuffer pDescriptorSetLayout = stack.longs(descriptorSetLayout);
            pipelineLayoutInfo.pSetLayouts(pDescriptorSetLayout);

            LongBuffer pPipelineLayout = stack.longs(0);

            int result = vkCreatePipelineLayout(mDevice, pipelineLayoutInfo, null, pPipelineLayout);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create pipeline layout! Err: %x", -result));
            }

            layout = pPipelineLayout.get(0);

            // Actual pipeline!

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo =
                    VkGraphicsPipelineCreateInfo.callocStack(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);

            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pColorBlendState(colorBlending);

            pipelineInfo.layout(layout);
            pipelineInfo.renderPass(renderPass);
            pipelineInfo.subpass(0);

            // We don't have base pipeline
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pPipeline = stack.longs(0);

            result =
                    vkCreateGraphicsPipelines(
                            mDevice, VK_NULL_HANDLE, pipelineInfo, null, pPipeline);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create graphics pipeline! Err: %x", -result));
            }

            fragShader.free();
            vertShader.free();

            pipeline = pPipeline.get(0);
        }
    }

    public void free() {
        vkDestroyPipeline(mDevice, pipeline, null);
        vkDestroyPipelineLayout(mDevice, layout, null);
    }
}
