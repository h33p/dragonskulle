/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.logging.Logger;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

import lombok.extern.java.Log;

/**
 * Class abstracting a vulkan pipeline
 *
 * <p>Normally, one pipeline is created per material, but there could be multiple passes.
 *
 * @author Aurimas Blažulionis
 */
@Log
class VulkanPipeline implements NativeResource {
    public long pipeline;
    public long layout;

    private VkDevice mDevice;
    private ShaderSet mShaderSet;

    /** Get vulkan binding descriptors for the vertex shader */
    private static VkVertexInputBindingDescription.Buffer getBindingDescriptions(
            MemoryStack stack, BindingDescription instanceBindingDescription) {

        BindingDescription[] bindingDescriptions = {
            Vertex.BINDING_DESCRIPTION, instanceBindingDescription
        };

        VkVertexInputBindingDescription.Buffer bindingDescriptionsOut =
                VkVertexInputBindingDescription.callocStack(bindingDescriptions.length, stack);
        for (int i = 0; i < bindingDescriptions.length; i++) {
            BindingDescription descIn = bindingDescriptions[i];
            VkVertexInputBindingDescription descOut = bindingDescriptionsOut.get(i);
            descOut.binding(descIn.bindingID);
            descOut.stride(descIn.size);
            descOut.inputRate(descIn.inputRate);
        }
        return bindingDescriptionsOut;
    }

    /** Get memory attribute descriptions for the vertex shader */
    private static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(
            MemoryStack stack, AttributeDescription... attributeDescriptions) {

        if (attributeDescriptions == null) {
            return getAttributeDescriptions(stack);
        }

        VkVertexInputAttributeDescription.Buffer attributeDescriptionsOut =
                VkVertexInputAttributeDescription.callocStack(
                        Vertex.ATTRIBUTE_DESCRIPTIONS.length + attributeDescriptions.length, stack);

        int cnt = 0;

        AttributeDescription[][] descs = {Vertex.ATTRIBUTE_DESCRIPTIONS, attributeDescriptions};

        for (AttributeDescription[] descArray : descs) {
            for (AttributeDescription descIn : descArray) {
                VkVertexInputAttributeDescription descOut = attributeDescriptionsOut.get(cnt++);
                descOut.binding(descIn.bindingID);
                descOut.location(descIn.location);
                descOut.format(descIn.format);
                descOut.offset(descIn.offset);
            }
        }

        return attributeDescriptionsOut;
    }

    public VulkanPipeline(
            ShaderSet shaderSet,
            long[] descriptorSetLayouts,
            VkDevice device,
            VkExtent2D extent,
            long renderPass) {
        log.fine("Setup pipeline");

        mDevice = device;
        mShaderSet = shaderSet;

        // This here will be used for stack allocation and keeping track of shader indices
        int shaderStageCount = 0;

        Shader vertShader = null;
        ShaderBuf vertShaderBuf = mShaderSet.getVertexShader();

        if (vertShaderBuf != null) {
            vertShader = Shader.getShader(vertShaderBuf, mDevice);

            if (vertShader == null) throw new RuntimeException("Failed to retrieve vertex shader!");
            shaderStageCount++;
        }

        Shader geomShader = null;
        ShaderBuf geomShaderBuf = mShaderSet.getGeometryShader();

        if (geomShaderBuf != null) {
            geomShader = Shader.getShader(geomShaderBuf, mDevice);

            if (geomShader == null) throw new RuntimeException("Failed to retrieve vertex shader!");
            shaderStageCount++;
        }

        Shader fragShader = null;
        ShaderBuf fragShaderBuf = mShaderSet.getFragmentShader();

        if (fragShaderBuf != null) {
            fragShader = Shader.getShader(fragShaderBuf, mDevice);

            if (fragShader == null)
                throw new RuntimeException("Failed to retrieve fragment shader!");
            shaderStageCount++;
        }

        try (MemoryStack stack = stackPush()) {

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo =
                    VkGraphicsPipelineCreateInfo.callocStack(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);

            // Programmable pipelines

            VkPipelineShaderStageCreateInfo.Buffer shaderStages =
                    VkPipelineShaderStageCreateInfo.callocStack(shaderStageCount, stack);

            // TODO: Use SpecializationEntry[] for pSpecializationInfo to configure constants
            if (vertShader != null) {
                VkPipelineShaderStageCreateInfo vertShaderStageInfo =
                        shaderStages.get(--shaderStageCount);
                vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
                vertShaderStageInfo.module(vertShader.getModule());
                vertShaderStageInfo.pName(stack.UTF8("main"));

                // Configure vertex pipeline
                VkPipelineVertexInputStateCreateInfo vertexInputInfo =
                        VkPipelineVertexInputStateCreateInfo.callocStack(stack);
                vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
                vertexInputInfo.pVertexBindingDescriptions(
                        getBindingDescriptions(stack, mShaderSet.getVertexBindingDescription()));
                vertexInputInfo.pVertexAttributeDescriptions(
                        getAttributeDescriptions(
                                stack, mShaderSet.getVertexAttributeDescriptions()));

                pipelineInfo.pVertexInputState(vertexInputInfo);
            }

            if (geomShader != null) {
                VkPipelineShaderStageCreateInfo geomShaderStageInfo =
                        shaderStages.get(--shaderStageCount);
                geomShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                geomShaderStageInfo.stage(VK_SHADER_STAGE_GEOMETRY_BIT);
                geomShaderStageInfo.module(geomShader.getModule());
                geomShaderStageInfo.pName(stack.UTF8("main"));
            }

            if (fragShader != null) {
                VkPipelineShaderStageCreateInfo fragShaderStageInfo =
                        shaderStages.get(--shaderStageCount);
                fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
                fragShaderStageInfo.module(fragShader.getModule());
                fragShaderStageInfo.pName(stack.UTF8("main"));
            }

            // Fixed function pipelines

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

            // TODO: Enable MSAA optionally/on feature check
            VkPipelineMultisampleStateCreateInfo multisampling =
                    VkPipelineMultisampleStateCreateInfo.callocStack(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            VkPipelineDepthStencilStateCreateInfo depthStencil =
                    VkPipelineDepthStencilStateCreateInfo.callocStack(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            if (shaderSet.isDepthTest()) {
                depthStencil.depthTestEnable(true);
                depthStencil.depthWriteEnable(true);
                depthStencil.depthCompareOp(VK_COMPARE_OP_LESS);
                depthStencil.depthBoundsTestEnable(false);
            }
            depthStencil.stencilTestEnable(false);

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                    VkPipelineColorBlendAttachmentState.callocStack(1, stack);
            colorBlendAttachment.colorWriteMask(
                    VK_COLOR_COMPONENT_R_BIT
                            | VK_COLOR_COMPONENT_G_BIT
                            | VK_COLOR_COMPONENT_B_BIT
                            | VK_COLOR_COMPONENT_A_BIT);

            if (shaderSet.isAlphaBlend()) {
                colorBlendAttachment.blendEnable(true);
                colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
                colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
                colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
                colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
                colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);
            }

            VkPipelineColorBlendStateCreateInfo colorBlending =
                    VkPipelineColorBlendStateCreateInfo.callocStack(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.pAttachments(colorBlendAttachment);

            // TODO: Dynamic states

            int fragmentConstantSize = mShaderSet.getFragmentPushConstantSize();

            int constantRanges = 1 + fragmentConstantSize > 0 ? 1 : 0;

            VkPushConstantRange.Buffer pushConstantRanges =
                    VkPushConstantRange.callocStack(constantRanges, stack);

            if (fragmentConstantSize > 0) {
                VkPushConstantRange fragmentConstantRange =
                        pushConstantRanges.get(--constantRanges);
                fragmentConstantRange.offset(0);
                fragmentConstantRange.size(fragmentConstantSize);
                fragmentConstantRange.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
            }

            VkPushConstantRange vertexConstantRange = pushConstantRanges.get(--constantRanges);
            vertexConstantRange.offset(0);
            vertexConstantRange.size(VertexConstants.SIZEOF);
            vertexConstantRange.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            VkPipelineLayoutCreateInfo pipelineLayoutInfo =
                    VkPipelineLayoutCreateInfo.callocStack(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);

            if (descriptorSetLayouts.length > 0) {
                LongBuffer pDescriptorSetLayout = stack.longs(descriptorSetLayouts);
                pipelineLayoutInfo.pSetLayouts(pDescriptorSetLayout);
            }

            pipelineLayoutInfo.pPushConstantRanges(pushConstantRanges);

            LongBuffer pPipelineLayout = stack.longs(0);

            int result = vkCreatePipelineLayout(mDevice, pipelineLayoutInfo, null, pPipelineLayout);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create pipeline layout! Err: %x", -result));
            }

            layout = pPipelineLayout.get(0);

            // Actual pipeline!

            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pDepthStencilState(depthStencil);
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

            if (fragShader != null) fragShader.free();
            if (geomShader != null) geomShader.free();
            if (vertShader != null) vertShader.free();

            pipeline = pPipeline.get(0);
        }
    }

    @Override
    public void free() {
        vkDestroyPipeline(mDevice, pipeline, null);
        vkDestroyPipelineLayout(mDevice, layout, null);
    }
}
