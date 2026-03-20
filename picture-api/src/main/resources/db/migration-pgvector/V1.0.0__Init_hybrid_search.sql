-- 混合检索扩展：为 picture_embeddings 表添加全文检索支持
-- 注意：此脚本需要在 PostgreSQL 中手动执行（LangChain4j 会自动创建基础表）

-- LangChain4j PgVectorEmbeddingStore 表结构：
-- id: UUID
-- embedding: vector (向量)
-- content: TEXT (原始文本内容)
-- metadata: JSONB (元数据)

-- 1. 确保 pg_trgm 扩展已安装（用于模糊匹配）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. 添加 tsvector 列（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'picture_embeddings'
        AND column_name = 'content_tsv'
    ) THEN
        ALTER TABLE picture_embeddings ADD COLUMN content_tsv tsvector;
    END IF;
END $$;

-- 3. 创建 GIN 索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_picture_embeddings_content_tsv
ON picture_embeddings USING gin(content_tsv);

-- 4. 创建触发器函数：自动更新 tsvector 列
-- 基于 content 列和 metadata 中的 name 字段
CREATE OR REPLACE FUNCTION update_picture_embedding_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.content_tsv :=
        setweight(to_tsvector('simple', COALESCE(NEW.content, '')), 'A') ||
        setweight(to_tsvector('simple', COALESCE(NEW.metadata->>'name', '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(NEW.metadata->>'pictureId', '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5. 创建触发器（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_update_picture_embedding_tsv'
    ) THEN
        CREATE TRIGGER trg_update_picture_embedding_tsv
        BEFORE INSERT OR UPDATE ON picture_embeddings
        FOR EACH ROW
        EXECUTE FUNCTION update_picture_embedding_tsv();
    END IF;
END $$;

-- 6. 为现有数据生成 tsvector（如果有数据）
UPDATE picture_embeddings
SET content_tsv =
    setweight(to_tsvector('simple', COALESCE(content, '')), 'A') ||
    setweight(to_tsvector('simple', COALESCE(metadata->>'name', '')), 'B') ||
    setweight(to_tsvector('simple', COALESCE(metadata->>'pictureId', '')), 'C')
WHERE content_tsv IS NULL;