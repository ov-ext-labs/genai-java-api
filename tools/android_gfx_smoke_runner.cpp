#include <cstdlib>
#include <filesystem>
#include <iostream>
#include <string>

#include <openvino/openvino.hpp>
#include <openvino/genai/generation_config.hpp>
#include <openvino/genai/llm_pipeline.hpp>

int main(int argc, char** argv) {
    if (argc < 3) {
        std::cerr << "usage: android_gfx_smoke_runner <model_dir> <prompt> [max_new_tokens]\n";
        return 2;
    }

    const std::filesystem::path model_dir = argv[1];
    const std::string prompt = argv[2];
    const size_t max_new_tokens = argc >= 4 ? static_cast<size_t>(std::strtoull(argv[3], nullptr, 10)) : 64;

    try {
        const auto model_xml = model_dir / "openvino_model.xml";
        std::cout << "[1/2] compile_model(" << model_xml.string() << ", GFX)" << std::endl;
        ov::Core core;
        auto compiled = core.compile_model(model_xml.string(), "GFX");
        (void)compiled;
        std::cout << "compiled model on GFX" << std::endl;

        std::cout << "[2/2] LLMPipeline(" << model_dir.string() << ", GFX)" << std::endl;
        ov::genai::LLMPipeline pipeline(model_dir, "GFX");
        std::cout << "pipeline created" << std::endl;
        ov::genai::GenerationConfig generation_config = pipeline.get_generation_config();
        std::cout << "generation config read" << std::endl;
        generation_config.max_new_tokens = max_new_tokens;

        std::cout << "generate begin" << std::endl;
        const auto result = pipeline.generate(prompt, generation_config);
        std::cout << "generate done" << std::endl;
        std::cout << result << std::endl;
        return 0;
    } catch (const std::exception& error) {
        std::cerr << "android_gfx_smoke_runner failed: " << error.what() << std::endl;
        return 1;
    }
}
